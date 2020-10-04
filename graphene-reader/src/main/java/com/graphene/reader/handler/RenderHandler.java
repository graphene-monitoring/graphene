package com.graphene.reader.handler;

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.graphene.reader.beans.TimeSeries;
import com.graphene.reader.config.RenderConfiguration;
import com.graphene.reader.exceptions.EvaluationException;
import com.graphene.reader.exceptions.InvalidParameterValueException;
import com.graphene.reader.exceptions.LogarithmicScaleNotAllowed;
import com.graphene.reader.exceptions.ParameterParsingException;
import com.graphene.reader.format.ResponseFormatter;
import com.graphene.reader.graphite.Target;
import com.graphene.reader.graphite.evaluation.EvaluationContext;
import com.graphene.reader.graphite.evaluation.TargetEvaluator;
import com.graphene.reader.graphite.evaluation.TargetVisitor;
import com.graphene.reader.graphite.grammar.GraphiteLexer;
import com.graphene.reader.graphite.grammar.GraphiteParser;
import com.graphene.reader.graphite.utils.ValueFormatter;
import com.graphene.reader.service.index.KeySearchHandler;
import com.graphene.reader.service.metric.DataFetchHandler;
import com.graphene.reader.service.stats.StatsService;
import com.graphene.reader.service.throttling.ThrottlingService;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * @author Andrei Ivanov
 */
@Component
public class RenderHandler {

  final static Logger logger = LogManager.getLogger(RenderHandler.class);

  private final TargetEvaluator evaluator;
  private final StatsService statsService;
  private final ThrottlingService throttlingService;
  private final RenderConfiguration renderConfiguration;

  private static final ExecutorService executor = Executors.newCachedThreadPool();
  private final TimeLimiter timeLimiter = SimpleTimeLimiter.create(executor);


  public RenderHandler(
    DataFetchHandler dataFetchHandler,
    KeySearchHandler keySearchHandler,
    StatsService statsService,
    ThrottlingService throttlingService,
    RenderConfiguration renderConfiguration
  ) {
    this.evaluator = new TargetEvaluator(dataFetchHandler, keySearchHandler);
    this.statsService = statsService;
    this.throttlingService = throttlingService;
    this.renderConfiguration = renderConfiguration;
  }

  public ResponseEntity<?> handle(RenderParameter parameters) throws ParameterParsingException {
    logger.debug("Render Got request: " + parameters + " / parameters: " + parameters.toString());
    Stopwatch timer = Stopwatch.createStarted();

    double throttled = throttlingService.throttle(parameters.getTenant());

    statsService.incRenderRequests(parameters.getTenant());

    if (throttled > 0) {
      statsService.incThrottleTime(parameters.getTenant(), throttled);
    }

    final List<Target> targets = new ArrayList<>();

    EvaluationContext context = new EvaluationContext(
      renderConfiguration.isHumanReadableNumbers() ? ValueFormatter.getInstance(parameters.getFormat()) : ValueFormatter.getInstance(ValueFormatter.ValueFormatterType.MACHINE)
    );

    // Let's parse the targets
    for (String targetString : parameters.getTargets()) {
      GraphiteLexer lexer = new GraphiteLexer(new ANTLRInputStream(targetString));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      GraphiteParser parser = new GraphiteParser(tokens);
      ParseTree tree = parser.expression();
      try {
        targets.add(new TargetVisitor(parameters.getTenant(), parameters.getFrom(), parameters.getUntil(), context).visit(tree));
      } catch (ParseCancellationException e) {
        String additionalInfo = null;
        if (e.getMessage() != null) additionalInfo = e.getMessage();
        if (e.getCause() != null) additionalInfo = e.getCause().getMessage();
        throw new InvalidParameterValueException("Could not parse target: " + targetString + " (" + additionalInfo + ")");
      }
    }

    logger.info("targets : " + targets);
    ResponseEntity<?> response;
    try {
      response = timeLimiter.callWithTimeout(
        (Callable<ResponseEntity>) () ->
          handleInternal(targets, parameters), renderConfiguration.getRequestTimeout(), TimeUnit.SECONDS);
    } catch (TimeoutException e) {
      logger.debug("Request timed out: " + parameters);
      statsService.incTimedOutRequests(parameters.getTenant());
      response = ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
        .build();
    } catch (Exception e) {
      logger.error(e);
      response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }

    timer.stop();
    logger.debug("Request took " + timer.elapsed(TimeUnit.MILLISECONDS) + " milliseconds (" + parameters + ")");

    logger.info("Response status : " + response.getStatusCode());
    return response;
  }

  private ResponseEntity<?> handleInternal(List<Target> targets, RenderParameter parameters) throws EvaluationException, LogarithmicScaleNotAllowed {
    // now evaluate each target producing list of TimeSeries
    List<TimeSeries> results = new ArrayList<>();

    for (Target target : targets) {
      List<TimeSeries> eval = evaluator.eval(target);
      results.addAll(eval);
    }

    return ResponseFormatter.formatResponse(results, parameters);
  }
}

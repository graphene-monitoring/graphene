package net.iponweb.disthene.service.store;

import com.datastax.driver.core.*;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.graphene.writer.input.GrapheneMetric;
import com.graphene.writer.config.StoreConfiguration;
import net.iponweb.disthene.events.StoreErrorEvent;
import net.iponweb.disthene.events.StoreSuccessEvent;
import net.iponweb.disthene.service.aggregate.CarbonConfiguration;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.Queue;
import java.util.concurrent.Executor;

/** @author Andrei Ivanov */
public class SingleWriterThread extends WriterThread {
  private Logger logger = Logger.getLogger(SingleWriterThread.class);

  private int rollup;
  private int period;

  public SingleWriterThread(
      String name,
      Session session,
      PreparedStatement statement,
      Queue<GrapheneMetric> metrics,
      Executor executor,
      StoreConfiguration storeConfiguration,
      CarbonConfiguration carbonConfiguration) {
    super(name, session, statement, metrics, executor);

    this.rollup = carbonConfiguration.getBaseRollup().getRollup();
    this.period = carbonConfiguration.getBaseRollup().getPeriod();
  }

  @Override
  public void run() {
    while (!shutdown) {
      GrapheneMetric metric = metrics.poll();
      if (metric != null) {
        store(metric);
      } else {
        try {
          Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
      }
    }
  }

  private void store(GrapheneMetric metric) {
    BoundStatement boundStatement =
        statement.bind(
            rollup * period,
            Collections.singletonList(metric.getValue()),
            metric.getTenant(),
            rollup,
            period,
            metric.getGraphiteKey(),
            metric.getTimestamp());
    ResultSetFuture future = session.executeAsync(boundStatement);

    Futures.addCallback(
        future,
        new FutureCallback<ResultSet>() {
          @Override
          public void onSuccess(ResultSet result) {
            // nothing
          }

          @SuppressWarnings("NullableProblems")
          @Override
          public void onFailure(Throwable t) {
            logger.error(t);
          }
        },
        executor);
  }
}

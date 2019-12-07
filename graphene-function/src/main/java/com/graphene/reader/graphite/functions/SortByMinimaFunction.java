package com.graphene.reader.graphite.functions;

import com.graphene.reader.exceptions.EvaluationException;
import com.graphene.reader.exceptions.InvalidArgumentException;
import com.graphene.reader.exceptions.TimeSeriesNotAlignedException;
import com.graphene.reader.graphite.Target;
import com.graphene.reader.graphite.evaluation.TargetEvaluator;
import com.graphene.reader.beans.TimeSeries;
import com.graphene.reader.utils.CollectionUtils;
import com.graphene.reader.utils.TimeSeriesUtils;

import java.util.*;

/**
 * @author Andrei Ivanov
 */
public class SortByMinimaFunction extends GrapheneFunction {


    public SortByMinimaFunction(String text) {
        super(text, "sortByMinima");
    }

    @Override
    public List<TimeSeries> evaluate(TargetEvaluator evaluator) throws EvaluationException {
        List<TimeSeries> processedArguments = new ArrayList<>();
        processedArguments.addAll(evaluator.eval((Target) arguments.get(0)));

        if (processedArguments.size() == 0) return new ArrayList<>();

        if (!TimeSeriesUtils.checkAlignment(processedArguments)) {
            throw new TimeSeriesNotAlignedException();
        }

        SortedMap<Double, List<TimeSeries>> sorted = new TreeMap<>();

        for(TimeSeries ts : processedArguments) {
            Double min = CollectionUtils.min(Arrays.asList(ts.getValues()));
            if (min == null) continue;
            if (sorted.get(min) == null) sorted.put(min, new ArrayList<TimeSeries>());
            sorted.get(min).add(ts);
        }

        List<TimeSeries> result = new ArrayList<>();

        for(Map.Entry<Double, List<TimeSeries>> entry : sorted.entrySet()) {
            result.addAll(entry.getValue());
        }

        return result;
    }

    @Override
    public void checkArguments() throws InvalidArgumentException {
        if (arguments.size() != 1) throw new InvalidArgumentException("sortByMinima: number of arguments is " + arguments.size() + ". Must be one.");
        if (!(arguments.get(0) instanceof Target)) throw new InvalidArgumentException("sortByMinima: argument is " + arguments.get(0).getClass().getName() + ". Must be series");
    }
}

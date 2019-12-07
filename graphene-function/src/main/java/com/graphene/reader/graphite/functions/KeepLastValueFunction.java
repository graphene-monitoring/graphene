package com.graphene.reader.graphite.functions;

import com.graphene.reader.exceptions.InvalidArgumentException;
import com.graphene.reader.exceptions.TimeSeriesNotAlignedException;
import com.graphene.reader.graphite.Target;
import com.graphene.reader.graphite.evaluation.TargetEvaluator;
import com.graphene.reader.beans.TimeSeries;
import com.graphene.reader.exceptions.EvaluationException;
import com.graphene.reader.utils.TimeSeriesUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Andrei Ivanov
 */
public class KeepLastValueFunction extends GrapheneFunction {

    public KeepLastValueFunction(String text) {
        super(text, "keepLastValue");
    }

    @Override
    public List<TimeSeries> evaluate(TargetEvaluator evaluator) throws EvaluationException {
        List<TimeSeries> processedArguments = new ArrayList<>();
        processedArguments.addAll(evaluator.eval((Target) arguments.get(0)));

        if (processedArguments.size() == 0) return new ArrayList<>();

        if (!TimeSeriesUtils.checkAlignment(processedArguments)) {
            throw new TimeSeriesNotAlignedException();
        }

        int limit = arguments.size() > 1 ? ((Double) arguments.get(1)).intValue() : Integer.MAX_VALUE;

        int length = processedArguments.get(0).getValues().length;

        for (TimeSeries ts : processedArguments) {
            Double lastValue = null;
            int nullCount = 0;
            for (int i = 0; i < length; i++) {
                if (ts.getValues()[i] != null) {
                    lastValue = ts.getValues()[i];
                    nullCount = 0;
                } else {
                    if (lastValue != null && nullCount <= limit) ts.getValues()[i] = lastValue;
                    nullCount++;
                }
            }
            ts.setName("keepLastValue(" + ts.getName() + "," + limit + ")");
        }

        return processedArguments;
    }

    @Override
    public void checkArguments() throws InvalidArgumentException {
        if (arguments.size() > 2 || arguments.size() < 1) throw new InvalidArgumentException("keepLastValue: number of arguments is " + arguments.size() + ". Must be one or two.");
        if (!(arguments.get(0) instanceof Target)) throw new InvalidArgumentException("keepLastValue: argument is " + arguments.get(0).getClass().getName() + ". Must be series");
        if (arguments.size() > 1 && !(arguments.get(1) instanceof Double)) throw new InvalidArgumentException("keepLastValue: argument is " + arguments.get(1).getClass().getName() + ". Must be a number");
    }
}

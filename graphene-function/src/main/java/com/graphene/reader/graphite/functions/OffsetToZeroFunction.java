package com.graphene.reader.graphite.functions;

import com.graphene.reader.beans.TimeSeries;
import com.graphene.reader.exceptions.EvaluationException;
import com.graphene.reader.exceptions.InvalidArgumentException;
import com.graphene.reader.exceptions.TimeSeriesNotAlignedException;
import com.graphene.reader.graphite.Target;
import com.graphene.reader.graphite.evaluation.TargetEvaluator;
import com.graphene.reader.utils.CollectionUtils;
import com.graphene.reader.utils.TimeSeriesUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Andrei Ivanov
 */
public class OffsetToZeroFunction extends GrapheneFunction {

    public OffsetToZeroFunction(String text) {
        super(text, "offsetToZero");
    }

    @Override
    public List<TimeSeries> evaluate(TargetEvaluator evaluator) throws EvaluationException {
        List<TimeSeries> processedArguments = new ArrayList<>();
        processedArguments.addAll(evaluator.eval((Target) arguments.get(0)));

        if (processedArguments.size() == 0) return new ArrayList<>();

        if (!TimeSeriesUtils.checkAlignment(processedArguments)) {
            throw new TimeSeriesNotAlignedException();
        }

        int length = processedArguments.get(0).getValues().length;

        for (TimeSeries ts : processedArguments) {
            Double offset = CollectionUtils.min(Arrays.asList(ts.getValues()));
            if (offset != null) {
                for (int i = 0; i < length; i++) {
                    if (ts.getValues()[i] != null ) {
                        ts.getValues()[i] -= offset;
                    }
                }
            }
            setResultingName(ts);
        }

        return processedArguments;
    }

    @Override
    public void checkArguments() throws InvalidArgumentException {
        if (arguments.size() > 1 || arguments.size() == 0) throw new InvalidArgumentException("offsetToZero: number of arguments is " + arguments.size() + ". Must be 1.");
        if (!(arguments.get(0) instanceof Target)) throw new InvalidArgumentException("offsetToZero: argument is " + arguments.get(0).getClass().getName() + ". Must be series");
    }
}

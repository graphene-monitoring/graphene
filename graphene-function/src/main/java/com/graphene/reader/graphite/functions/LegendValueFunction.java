package com.graphene.reader.graphite.functions;

import com.graphene.reader.beans.TimeSeries;
import com.graphene.reader.exceptions.EvaluationException;
import com.graphene.reader.exceptions.InvalidArgumentException;
import com.graphene.reader.exceptions.TimeSeriesNotAlignedException;
import com.graphene.reader.graphite.Target;
import com.graphene.reader.graphite.evaluation.TargetEvaluator;
import com.graphene.reader.utils.UnitSystem;
import com.graphene.reader.graphite.utils.ValueFormatter;
import com.graphene.reader.utils.CollectionUtils;
import com.graphene.reader.utils.TimeSeriesUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Andrei Ivanov
 */
public class LegendValueFunction extends GrapheneFunction {

    public LegendValueFunction(String text) {
        super(text, "legendValue");
    }

    @Override
    public List<TimeSeries> evaluate(TargetEvaluator evaluator) throws EvaluationException {
        List<TimeSeries> processedArguments = new ArrayList<>();
        processedArguments.addAll(evaluator.eval((Target) arguments.get(0)));

        if (processedArguments.size() == 0) return new ArrayList<>();

        if (!TimeSeriesUtils.checkAlignment(processedArguments)) {
            throw new TimeSeriesNotAlignedException();
        }

        UnitSystem unitSystem = UnitSystem.NONE;
        List<String> aggregations = new ArrayList<>();
        for (int i = 1; i < arguments.size(); i++) {
            String argument = ((String) arguments.get(i)).toLowerCase().replaceAll("[\"\']", "");
            if (argument.equals("last") || argument.equals("avg") || argument.equals("total") || argument.equals("min") || argument.equals("max")) {
                aggregations.add(argument);
            } else {
                unitSystem = UnitSystem.valueOf(argument.replaceAll("^\"|\"$", "").toUpperCase());
            }
        }

        ValueFormatter formatter = getContext().getFormatter();

        for (TimeSeries ts : processedArguments) {
            StringBuilder sb = new StringBuilder("");
            List<Double> valuesArray = Arrays.asList(ts.getValues());
            for (String aggregation : aggregations) {
                switch (aggregation) {
                    case "last": {
                        Double v = CollectionUtils.last(valuesArray);
                        if (v != null) {
                            sb.append(" (last: ").append(formatter.formatValue(v, unitSystem)).append(")");
                        }
                        break;
                    }
                    case "avg": {
                        Double v = CollectionUtils.average(valuesArray);
                        if (v != null) {
                            sb.append(" (avg: ").append(formatter.formatValue(v, unitSystem)).append(")");
                        }
                        break;
                    }
                    case "total": {
                        Double v = CollectionUtils.sum(valuesArray);
                        if (v != null) {
                            sb.append(" (total: ").append(formatter.formatValue(v, unitSystem)).append(")");
                        }
                        break;
                    }
                    case "min": {
                        Double v = CollectionUtils.min(valuesArray);
                        if (v != null) {
                            sb.append(" (min: ").append(formatter.formatValue(v, unitSystem)).append(")");
                        }
                        break;
                    }
                    case "max": {
                        Double v = CollectionUtils.max(valuesArray);
                        if (v != null) {
                            sb.append(" (max: ").append(formatter.formatValue(v, unitSystem)).append(")");
                        }
                        break;
                    }
                }

            }
            ts.setName(ts.getName() + sb.toString());
        }

        return processedArguments;
    }

    @Override
    public void checkArguments() throws InvalidArgumentException {
        if (arguments.size() < 2)
            throw new InvalidArgumentException("legendValue: number of arguments is " + arguments.size() + ". Must at least two.");
        if (!(arguments.get(0) instanceof Target))
            throw new InvalidArgumentException("legendValue: argument is " + arguments.get(0).getClass().getName() + ". Must be series");

        for (int i = 1; i < arguments.size(); i++) {
            if (!(arguments.get(i) instanceof String))
                throw new InvalidArgumentException("legendValue: argument is " + arguments.get(i).getClass().getName() + ". Must be a string");

            String argument = ((String) arguments.get(i)).toLowerCase().replaceAll("[\"\']", "");

            if (!argument.equals("last") && !argument.equals("avg") && !argument.equals("total") && !argument.equals("min") && !argument.equals("max")) {
                if ((i != arguments.size() - 1) || (i == 1))
                    throw new InvalidArgumentException("legendValue: there must be at least one aggregation.");

                try {
                    UnitSystem.valueOf(((String) arguments.get(i)).toUpperCase().replaceAll("^\"|\"$", ""));
                } catch (IllegalArgumentException e) {
                    throw new InvalidArgumentException("legendValue: unknown unit system.");
                }
            }
        }

    }
}

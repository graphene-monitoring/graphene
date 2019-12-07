package com.graphene.reader.config;

/**
 * @author Andrei Ivanov
 */
public class Rollup {

    private int rollup;
    private int period;

    public Rollup(String s) {
        String[] ss = s.split(":");
        rollup = Integer.parseInt(ss[0].substring(0, ss[0].length() - 1));
        period = (int) (Long.parseLong(ss[1].substring(0, ss[1].length() - 1)) / rollup);
    }

    public int getRollup() {
        return rollup;
    }

    public void setRollup(int rollup) {
        this.rollup = rollup;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    @Override
    public String toString() {
        return "Rollup{" +
                "rollup=" + rollup +
                ", period=" + period +
                '}';
    }
}

package com.graphene.reader.utils;

import com.google.common.math.DoubleMath;
import com.graphene.reader.graphite.utils.ValueFormatter;
import com.graphene.reader.graphite.utils.ValueFormatter;

import java.math.BigDecimal;

/**
 * @author Andrei Ivanov
 */
public class MachineValueFormatter extends ValueFormatter {

  @Override
  protected String formatDoubleSpecialSmart(Double value) {
    BigDecimal bigDecimal = BigDecimal.valueOf(value);

    // do not do this for math integers
    if (!DoubleMath.isMathematicalInteger(value)) {
      // precision is just like in graphite (scale check redundant but let it be)
      if (bigDecimal.precision() > 12 && bigDecimal.scale() > 0) {
        int roundTo = bigDecimal.scale() - bigDecimal.precision() + 12 > 0 ? bigDecimal.scale() - bigDecimal.precision() + 12 : 0;
        bigDecimal = bigDecimal.setScale(roundTo, BigDecimal.ROUND_HALF_UP);
      }
    }


    return (bigDecimal.precision() + bigDecimal.scale() > 12) ?
      bigDecimal.stripTrailingZeros().toEngineeringString() : bigDecimal.stripTrailingZeros().toPlainString();

  }
}

package com.linkedin.thirdeye.bootstrap.rollup;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.linkedin.thirdeye.bootstrap.MetricTimeSeries;
/**
 * 
 * @author kgopalak
 *
 */
public class TotalAggregateBasedRollupFunction implements RollupThresholdFunc{
  private static final Logger LOG = LoggerFactory
      .getLogger(TotalAggregateBasedRollupFunction.class);
  private String metricName;
  private int totalAggregateThreshold;
  public TotalAggregateBasedRollupFunction(String metricName, int totalAggregateThreshold){
    this.metricName = metricName;
    this.totalAggregateThreshold = totalAggregateThreshold;
  }
  /**
   * 
   */
  @Override
  public boolean isAboveThreshold(MetricTimeSeries timeSeries) {
    Set<Long> timeWindowSet = timeSeries.getTimeWindowSet();
    long sum = 0;
    for (Long timeWindow : timeWindowSet) {
      sum += timeSeries.get(timeWindow, metricName).longValue();
    }
    LOG.info("sum = " + sum);
    return sum  >= totalAggregateThreshold; 
  }

}

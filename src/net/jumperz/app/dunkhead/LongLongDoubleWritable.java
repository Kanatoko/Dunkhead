package net.jumperz.app.dunkhead;

import java.io.*;

import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;

public class LongLongDoubleWritable implements WritableComparable {
  private long time;
  private long count;
  private double value;

  public LongLongDoubleWritable() {}

  public LongLongDoubleWritable( long time, long count, double d ) { set( time, count ,d ); }

  /** Set the value of this LongDoubleWritable. */
  public void set( long time, long count, double d ) { this.time = time; this.count = count; this.value = d; }

  /** Return the value of this LongDoubleWritable. */
  public long getTime() { return time; }
  public double getValue(){ return value; }
  public long getCount() { return count; }

  public void readFields(DataInput in) throws IOException {
    time = in.readLong();
    count = in.readLong();
    value = in.readDouble();
  }

  public void write(DataOutput out) throws IOException {
    out.writeLong(time);
    out.writeLong(count);
    out.writeDouble(value);
  }

  /** Returns true iff <code>o</code> is a LongDoubleWritable with the same value. */
  public boolean equals(Object o) {
    if (!(o instanceof LongLongDoubleWritable))
      return false;
    LongLongDoubleWritable other = (LongLongDoubleWritable)o;
    return ( this.time == other.time && this.value == other.value && this.count == other.count );
  }

  public int hashCode() {
    return ( int )time + ( int )value + ( int )count;
  }

  /** Compares two LongDoubleWritables. */
  public int compareTo(Object o) {
    double thisValue = this.time + this.value + this.count;
    double thatValue = ((LongLongDoubleWritable)o).time + ((LongLongDoubleWritable)o).value + ((LongLongDoubleWritable)o).count;;
    return (thisValue<thatValue ? -1 : (thisValue==thatValue ? 0 : 1));
  }

  public String toString() {
    return time + ":" + count + ":" + value;
  }

  /** A Comparator optimized for LongDoubleWritable. */ 
  public static class Comparator extends WritableComparator {
    public Comparator() {
      super(LongLongDoubleWritable.class);
    }

    public int compare(byte[] b1, int s1, int l1,
                       byte[] b2, int s2, int l2) {
      long thisValue = readLong(b1, s1);
      long thatValue = readLong(b2, s2);
      return (thisValue<thatValue ? -1 : (thisValue==thatValue ? 0 : 1));
    }
  }

  static {                                        // register this comparator
    WritableComparator.define(LongLongDoubleWritable.class, new Comparator());
  }
}


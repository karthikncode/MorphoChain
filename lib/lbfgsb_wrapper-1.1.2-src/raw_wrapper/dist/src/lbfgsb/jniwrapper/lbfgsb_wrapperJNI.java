/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 2.0.11
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package lbfgsb.jniwrapper;

public class lbfgsb_wrapperJNI {
  public final static native long new_intArray(int jarg1);
  public final static native void delete_intArray(long jarg1);
  public final static native int intArray_getitem(long jarg1, int jarg2);
  public final static native void intArray_setitem(long jarg1, int jarg2, int jarg3);
  public final static native long new_doubleArray(int jarg1);
  public final static native void delete_doubleArray(long jarg1);
  public final static native double doubleArray_getitem(long jarg1, int jarg2);
  public final static native void doubleArray_setitem(long jarg1, int jarg2, double jarg3);
  public final static native int LBFGSB_TASK_SIZE_get();
  public final static native void lbfgsb_n_set(long jarg1, lbfgsb jarg1_, int jarg2);
  public final static native int lbfgsb_n_get(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_m_set(long jarg1, lbfgsb jarg1_, int jarg2);
  public final static native int lbfgsb_m_get(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_x_set(long jarg1, lbfgsb jarg1_, long jarg2);
  public final static native long lbfgsb_x_get(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_l_set(long jarg1, lbfgsb jarg1_, long jarg2);
  public final static native long lbfgsb_l_get(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_u_set(long jarg1, lbfgsb jarg1_, long jarg2);
  public final static native long lbfgsb_u_get(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_nbd_set(long jarg1, lbfgsb jarg1_, long jarg2);
  public final static native long lbfgsb_nbd_get(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_f_set(long jarg1, lbfgsb jarg1_, double jarg2);
  public final static native double lbfgsb_f_get(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_g_set(long jarg1, lbfgsb jarg1_, long jarg2);
  public final static native long lbfgsb_g_get(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_factr_set(long jarg1, lbfgsb jarg1_, double jarg2);
  public final static native double lbfgsb_factr_get(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_pgtol_set(long jarg1, lbfgsb jarg1_, double jarg2);
  public final static native double lbfgsb_pgtol_get(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_wa_set(long jarg1, lbfgsb jarg1_, long jarg2);
  public final static native long lbfgsb_wa_get(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_iwa_set(long jarg1, lbfgsb jarg1_, long jarg2);
  public final static native long lbfgsb_iwa_get(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_task_set(long jarg1, lbfgsb jarg1_, String jarg2);
  public final static native String lbfgsb_task_get(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_iprint_set(long jarg1, lbfgsb jarg1_, int jarg2);
  public final static native int lbfgsb_iprint_get(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_csave_set(long jarg1, lbfgsb jarg1_, String jarg2);
  public final static native String lbfgsb_csave_get(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_lsave_set(long jarg1, lbfgsb jarg1_, long jarg2);
  public final static native long lbfgsb_lsave_get(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_isave_set(long jarg1, lbfgsb jarg1_, long jarg2);
  public final static native long lbfgsb_isave_get(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_dsave_set(long jarg1, lbfgsb jarg1_, long jarg2);
  public final static native long lbfgsb_dsave_get(long jarg1, lbfgsb jarg1_);
  public final static native long new_lbfgsb();
  public final static native void delete_lbfgsb(long jarg1);
  public final static native long lbfgsb_create(int jarg1, int jarg2);
  public final static native void lbfgsb_delete(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_step(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_set_task(long jarg1, lbfgsb jarg1_, int jarg2);
  public final static native int lbfgsb_get_task(long jarg1, lbfgsb jarg1_);
  public final static native void lbfgsb_set_task_str(long jarg1, lbfgsb jarg1_, String jarg2);
  public final static native int lbfgsb_is_task_str_equal(long jarg1, lbfgsb jarg1_, String jarg2);
}
/*
 * Author:Wang Lei
 */

package com.bird.ninekeylock;

public interface NineKeyLockListener {
	public void beforeInput(NineKeyLockView nineKeyLockView);
	public void afterInput(NineKeyLockView nineKeyLockView);
	public void beforeCheck(NineKeyLockView nineKeyLockView, boolean passed);
	public void afterCheck(NineKeyLockView nineKeyLockView, boolean passed);
	public void onAddHit(NineKeyLockView nineKeyLockView);
}
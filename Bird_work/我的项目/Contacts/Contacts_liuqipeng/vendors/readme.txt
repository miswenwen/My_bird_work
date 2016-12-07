This folder contains source code folders for different vendors, e.g. MTK, QCOM, SPREADTRUM, etc.
Some vendor-dependent interfaces are called here.
For each new vendor, we need to:
1. Create a sub-folder in this folder for source code specific to the vendor.
2. Check the $(YUNOS_PLATFORM) against the vendor name in Android.mk and add the folder created in step 1 to EXTRA_VENDOR_SOURCE.
3. Create a class named VendorSimImpl, and implements com.yunos.alicontacts.sim.ICrossPlatformSim.
4. Modify SimUtil.java, and initialize some (SIM related) constants with proper values in the static block.

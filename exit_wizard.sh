#!/bin/bash

adb root
wait ${!}
adb shell pm enable org.mokee.setupwizard/org.mokee.setupwizard.SetupWizardExitActivity || true
wait ${!}
adb shell pm enable com.google.android.setupwizard/com.google.android.setupwizard.SetupWizardExitActivity || true
wait ${!}
sleep 1
adb shell am start org.mokee.setupwizard/org.mokee.setupwizard.SetupWizardExitActivity || true
wait ${!}
sleep 1
adb shell am start com.google.android.setupwizard/com.google.android.setupwizard.SetupWizardExitActivity

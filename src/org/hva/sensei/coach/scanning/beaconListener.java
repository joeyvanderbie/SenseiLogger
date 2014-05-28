package org.hva.sensei.coach.scanning;

import org.hva.sensei.coach.beacon.AbstractBeacon;

public interface beaconListener {
	public void beaconFound(AbstractBeacon b);
	public void scanningStarted();
	public void scanningStopped();
}

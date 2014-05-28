package org.hva.sensei.coach.configuring;

import org.hva.sensei.coach.beacon.BeaconMessage;

public interface BeaconConnectionListener {
	public void beaconConnected();
	public void beaconSystemDisconnected();
	public void beaconUserDisconnected();
	public void dataReceived(BeaconMessage bm);
}

package com.bitwig.extensions.controllers.mackie.old;

import java.util.ArrayList;
import java.util.List;

import com.bitwig.extension.controller.api.Bank;
import com.bitwig.extension.controller.api.Parameter;

public class FlippableBankLayer extends FlippableLayer {

	private final List<Bank<? extends Parameter>> bankList = new ArrayList<>();

	public FlippableBankLayer(final ChannelSection section, final String name) {
		super(section, name);
	}

	@Override
	public void navigateLeftRight(final int direction) {
		if (!isActive()) {
			return;
		}
		for (final Bank<?> bank : bankList) {
			if (direction > 0 && bank.canScrollForwards().get()) {
				bank.scrollForwards();
			} else if (direction < 0 && bank.canScrollBackwards().get()) {
				bank.scrollBackwards();
			}
		}
	}

	public void addBank(final Bank<? extends Parameter> bank) {
		bank.canScrollForwards().markInterested();
		bank.canScrollBackwards().markInterested();
		bankList.add(bank);
	}

}

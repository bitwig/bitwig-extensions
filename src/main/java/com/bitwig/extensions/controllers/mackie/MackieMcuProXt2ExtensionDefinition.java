package com.bitwig.extensions.controllers.mackie;

import java.util.UUID;

public class MackieMcuProXt2ExtensionDefinition extends MackieMcuProExtensionDefinition {
	private static final UUID DRIVER_ID = UUID.fromString("fa145533-5f45-4e19-81ad-1de77ffa2df1");

	public MackieMcuProXt2ExtensionDefinition() {
		super(2);
	}

	@Override
	public UUID getId() {
		return DRIVER_ID;
	}

}

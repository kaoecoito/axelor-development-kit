package com.axelor.web;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.servlet.GuiceServletContextListener;

/**
 * Servlet context listener.
 *
 */
public class AppContextListener  extends GuiceServletContextListener {
	
	@Override
	protected final Injector getInjector() {
		//TODO: auto discover Guice modules within `com.axelor` namespace
		return Guice.createInjector(new AppServletModule());
	}
}
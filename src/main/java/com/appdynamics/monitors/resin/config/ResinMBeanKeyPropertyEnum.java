/*
 * Copyright 2014. AppDynamics LLC and its affiliates.
 *  All Rights Reserved.
 *  This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *  The copyright notice above does not evidence any actual or intended publication of such source code.
 */

package com.appdynamics.monitors.resin.config;

public enum ResinMBeanKeyPropertyEnum {
    TYPE("type"),
    HOST("Host"),
    WEBAPP("WebApp"),
    NAME("name");
	
	private String name;
	
	private ResinMBeanKeyPropertyEnum(String name) {
		this.name = name;
	}
	
	public String toString(){
        return name;
    }
}

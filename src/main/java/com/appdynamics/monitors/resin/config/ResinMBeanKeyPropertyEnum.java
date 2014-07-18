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

package com.dabsquared.gitlabjenkins.gitlab.api.model;

/**
 * Project Viibility Type (Used on Fetch Group Projects) 
 * @author <a href="mailto:jetune@kube-cloud.com">Jean-Jacques ETUNE NGI (Java EE Technical Lead / Enterprise Architect)</a>
 * @since Mon, 2022-06-13 - 10:33:47
 */
public enum ProjectVisibilityType {
	
	/**
	 * Public Access
	 */
	PUBLIC("public"),
	
	/**
	 * Internal Access
	 */
	INTERNAL("internal"),

	/**
	 * Private Access
	 */
	PRIVATE("private");
	
	/**
	 * Enumeration Value
	 */
	private String value;
	
	/**
	 * Constructor with parameters
	 * @param value	Enumeration Value
	 */
	private ProjectVisibilityType(String value) {
		
		// Initialize Value
		this.value = value;
	}

	/**
	 * Method used to get the value of field "value"
	 * @return Value of field "value"
	 */
	public String getValue() {
	
		// Return value
		return value;
	}
}

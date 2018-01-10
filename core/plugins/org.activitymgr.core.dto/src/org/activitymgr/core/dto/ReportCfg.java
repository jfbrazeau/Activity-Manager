package org.activitymgr.core.dto;

import org.activitymgr.core.orm.annotation.Column;
import org.activitymgr.core.orm.annotation.ColumnNamePrefix;
import org.activitymgr.core.orm.annotation.Table;

@Table("REPORT_CONFIG")
@ColumnNamePrefix("REP_")
public class ReportCfg extends SimpleIdentityBean {

	/** Category. */
	private String category;

	/** Owner identifier. */
	@Column("OWNER")
	private Long ownerId;

	/** Name. */
	private String name;

	/** Owner identifier. */
	private String configuration;

	/**
	 * Returns the category.
	 * 
	 * @return the category.
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * Sets the category.
	 * 
	 * @param category
	 *            the new category.
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * Returns the owner identifier.
	 * 
	 * @return the owner identifier.
	 */
	public Long getOwnerId() {
		return ownerId;
	}

	/**
	 * Sets the owner identifier.
	 * 
	 * @param ownerId
	 *            the owner identifier.
	 */
	public void setOwnerId(Long ownerId) {
		this.ownerId = ownerId;
	}

	/**
	 * Returns the name.
	 * 
	 * @return the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name.
	 * 
	 * @param name
	 *            the new name.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the configuration.
	 * 
	 * @return the configuration.
	 */
	public String getConfiguration() {
		return configuration;
	}

	/**
	 * Sets the configuration.
	 * 
	 * @param configuration
	 *            the new configuration.
	 */
	public void setConfiguration(String configuration) {
		this.configuration = configuration;
	}

}
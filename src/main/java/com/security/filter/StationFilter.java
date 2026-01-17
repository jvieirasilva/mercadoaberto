package com.security.filter;

import java.util.List;

public class StationFilter {
	
	    private int page;
	    private int size;
	    private List<String> serialModes;
	    private List<String> serialValues;
	    private List<String>  statusModes;
	    private List<String>  statusValues;
	    private String onlineStatus;
	    private List<String> modelModes;
	    private List<String> modelValues;
	    private List<String> dateMatchModes;
	    private List<String> dateValues;
	    private List<String> installationDate;
	    private List<String> installationDateEnd;
	    
		public int getPage() {
			return page;
		}
		public void setPage(int page) {
			this.page = page;
		}
		public int getSize() {
			return size;
		}
		public void setSize(int size) {
			this.size = size;
		}
		public List<String> getSerialModes() {
			return serialModes;
		}
		public void setSerialModes(List<String> serialModes) {
			this.serialModes = serialModes;
		}
		public List<String> getSerialValues() {
			return serialValues;
		}
		public void setSerialValues(List<String> serialValues) {
			this.serialValues = serialValues;
		}
		public List<String> getStatusModes() {
			return statusModes;
		}
		public void setStatusModes(List<String> statusModes) {
			this.statusModes = statusModes;
		}
		public List<String> getStatusValues() {
			return statusValues;
		}
		public void setStatusValues(List<String> statusValues) {
			this.statusValues = statusValues;
		}
		public String getOnlineStatus() {
			return onlineStatus;
		}
		public void setOnlineStatus(String onlineStatus) {
			this.onlineStatus = onlineStatus;
		}
		public List<String> getModelModes() {
			return modelModes;
		}
		public void setModelModes(List<String> modelModes) {
			this.modelModes = modelModes;
		}
		public List<String> getModelValues() {
			return modelValues;
		}
		public void setModelValues(List<String> modelValues) {
			this.modelValues = modelValues;
		}
		public List<String> getDateMatchModes() {
			return dateMatchModes;
		}
		public void setDateMatchModes(List<String> dateMatchModes) {
			this.dateMatchModes = dateMatchModes;
		}
		public List<String> getDateValues() {
			return dateValues;
		}
		public void setDateValues(List<String> dateValues) {
			this.dateValues = dateValues;
		}
		public List<String> getInstallationDate() {
			return installationDate;
		}
		public void setInstallationDate(List<String> installationDate) {
			this.installationDate = installationDate;
		}
		public List<String> getInstallationDateEnd() {
			return installationDateEnd;
		}
		public void setInstallationDateEnd(List<String> installationDateEnd) {
			this.installationDateEnd = installationDateEnd;
		}
		
		
		
}

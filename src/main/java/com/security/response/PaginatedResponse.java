package com.security.response;


import java.util.List;

public class PaginatedResponse<T> {
    private List<T> content;
    private long totalElements;
    private int pageNumber;
    private int pageSize;
    
    public int getTotalPages() {
        return (int) Math.ceil((double) totalElements / pageSize);
    }
    
    public boolean hasNext() {
        return pageNumber < getTotalPages() - 1;
    }
    
    public boolean hasPrevious() {
        return pageNumber > 0;
    }

	public List<T> getContent() {
		return content;
	}

	public void setContent(List<T> content) {
		this.content = content;
	}

	public long getTotalElements() {
		return totalElements;
	}

	public void setTotalElements(long totalElements) {
		this.totalElements = totalElements;
	}

	public int getPageNumber() {
		return pageNumber;
	}

	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public PaginatedResponse(List<T> content, long totalElements, int pageNumber, int pageSize) {
		super();
		this.content = content;
		this.totalElements = totalElements;
		this.pageNumber = pageNumber;
		this.pageSize = pageSize;
	}
    
	
	
    
    
    
}
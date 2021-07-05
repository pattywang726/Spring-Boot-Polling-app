package com.example.polls.payload;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
// Pagination consist of two fields – page size and page number
public class PagedResponse<T> {
    private List<T> content;
    private int page; // page number of the current page. Default is 0.
    private int size; // page size.
    private long totalElements; // total Polls listed, for example
    private int totalPages; // total pages to store these Polls.
    private boolean last;
}

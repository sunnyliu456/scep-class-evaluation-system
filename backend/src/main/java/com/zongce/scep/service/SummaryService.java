package com.zongce.scep.service;

import com.zongce.scep.dto.StudentSummaryDto;

import java.util.List;

public class SummaryService {

    private final RosterService rosterService;

    public SummaryService(RosterService rosterService) {
        this.rosterService = rosterService;
    }

    public List<StudentSummaryDto> listAll() {
        return rosterService.current();
    }
}

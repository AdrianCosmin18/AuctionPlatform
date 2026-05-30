package org.nedelcu.cosmin.auction.api.auction.scheduler;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nedelcu.cosmin.auction.api.auction.model.AuctionStatus;
import org.nedelcu.cosmin.auction.api.auction.repository.AuctionRepository;
import org.nedelcu.cosmin.auction.api.auction.service.AuctionService;

@ExtendWith(MockitoExtension.class)
class AuctionSchedulerTest {

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private AuctionService auctionService;

    @InjectMocks
    private AuctionScheduler auctionScheduler;

    @Test
    void closeExpiredAuctionsDelegatesAllExpiredAuctionIds() {
        when(auctionRepository.findExpiredAuctionIds(
                org.mockito.ArgumentMatchers.eq(AuctionStatus.RUNNING),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(List.of(10L, 11L));

        auctionScheduler.closeExpiredAuctions();

        verify(auctionService).closeExpiredAuction(10L);
        verify(auctionService).closeExpiredAuction(11L);
    }

    @Test
    void closeExpiredAuctionsContinuesWhenClosingOneAuctionFails() {
        when(auctionRepository.findExpiredAuctionIds(
                org.mockito.ArgumentMatchers.eq(AuctionStatus.RUNNING),
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(List.of(10L, 11L));
        doThrow(new RuntimeException("boom")).when(auctionService).closeExpiredAuction(10L);

        auctionScheduler.closeExpiredAuctions();

        verify(auctionService, times(1)).closeExpiredAuction(10L);
        verify(auctionService, times(1)).closeExpiredAuction(11L);
    }
}

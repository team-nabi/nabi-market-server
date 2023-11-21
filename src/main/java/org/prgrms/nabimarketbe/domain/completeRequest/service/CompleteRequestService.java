package org.prgrms.nabimarketbe.domain.completeRequest.service;

import org.prgrms.nabimarketbe.domain.card.entity.Card;
import org.prgrms.nabimarketbe.domain.card.repository.CardRepository;
import org.prgrms.nabimarketbe.domain.completeRequest.dto.request.CompleteRequestDTO;
import org.prgrms.nabimarketbe.domain.completeRequest.dto.response.CompleteRequestResponseDTO;
import org.prgrms.nabimarketbe.domain.completeRequest.dto.response.wrapper.HistoryListReadLimitResponseDTO;
import org.prgrms.nabimarketbe.domain.completeRequest.dto.response.wrapper.HistoryListReadPagingResponseDTO;
import org.prgrms.nabimarketbe.domain.completeRequest.entity.CompleteRequest;
import org.prgrms.nabimarketbe.domain.completeRequest.repository.CompleteRequestRepository;
import org.prgrms.nabimarketbe.domain.suggestion.entity.Suggestion;
import org.prgrms.nabimarketbe.domain.suggestion.repository.SuggestionRepository;
import org.prgrms.nabimarketbe.domain.user.entity.User;
import org.prgrms.nabimarketbe.domain.user.repository.UserRepository;
import org.prgrms.nabimarketbe.domain.user.service.CheckService;
import org.prgrms.nabimarketbe.global.error.BaseException;
import org.prgrms.nabimarketbe.global.error.ErrorCode;
import org.prgrms.nabimarketbe.global.event.NotificationCreateEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CompleteRequestService {
    private final CompleteRequestRepository completeRequestRepository;

    private final SuggestionRepository suggestionRepository;

    private final UserRepository userRepository;

    private final CardRepository cardRepository;

    private final CheckService checkService;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public CompleteRequestResponseDTO createCompleteRequest(
        String token,
        CompleteRequestDTO requestDTO
    ) {
        User user = userRepository.findById(checkService.parseToken(token))
            .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        Card fromCard = cardRepository.findByCardIdAndUser(requestDTO.fromCardId(), user)
            .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_MATCHED));

        Card toCard = cardRepository.findById(requestDTO.toCardId())
            .orElseThrow(() -> new BaseException(ErrorCode.CARD_NOT_FOUND));

        Suggestion suggestion = suggestionRepository.findSuggestionByFromCardAndToCard(fromCard, toCard)
            .orElseThrow(() -> new BaseException(ErrorCode.SUGGESTION_NOT_FOUND));

        if (!suggestion.isAccepted()) {
            throw new BaseException(ErrorCode.SUGGESTION_NOT_ACCEPTED);
        }

        if (checkService.isEqual(user.getUserId(), toCard.getUser().getUserId())) {
            throw new BaseException(ErrorCode.COMPLETE_REQUEST_MYSELF_ERROR);
        }

        CompleteRequest completeRequest = new CompleteRequest(fromCard, toCard);

        CompleteRequest savedCompleteRequest = completeRequestRepository.save(completeRequest);

        createCompleteRequestEvent(savedCompleteRequest);

        return CompleteRequestResponseDTO.from(savedCompleteRequest);
    }

    @Transactional
    public CompleteRequestResponseDTO updateCompleteRequestStatus(
        String token,
        Long fromCardId,
        Long toCardId,
        Boolean isAccepted
    ) {
        Long userId = checkService.parseToken(token);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        Card fromCard = cardRepository.findById(fromCardId)
            .orElseThrow(() -> new BaseException(ErrorCode.CARD_NOT_FOUND));

        Card toCard = cardRepository.findByCardIdAndUser(toCardId, user)
            .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_MATCHED));

        CompleteRequest completeRequest = completeRequestRepository
            .findCompleteRequestByFromCardAndToCard(fromCard, toCard)
            .orElseThrow(() -> new BaseException(ErrorCode.COMPLETE_REQUEST_NOT_FOUND));

        if (!checkService.isEqual(userId, fromCard.getUser().getUserId()) &&
            !checkService.isEqual(userId, toCard.getUser().getUserId())) {
            throw new BaseException(ErrorCode.USER_NOT_MATCHED);
        }

        updateStatus(isAccepted, completeRequest, fromCard, toCard);

        createCompleteRequestDecisionEvent(completeRequest, isAccepted);

        return CompleteRequestResponseDTO.from(completeRequest);
    }

    @Transactional(readOnly = true)
    public HistoryListReadLimitResponseDTO getHistoryBySize(Integer size) {
        return completeRequestRepository.getHistoryBySize(size);
    }

    @Transactional(readOnly = true)
    public HistoryListReadPagingResponseDTO getHistoryByUser(
        String token,
        String cursorId,
        Integer size
    ) {
        User user = userRepository.findById(checkService.parseToken(token))
            .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        return completeRequestRepository.getHistoryByUser(user, cursorId, size);
    }

    private void updateStatus(
        Boolean isAccepted,
        CompleteRequest completeRequest,
        Card fromCard,
        Card toCard
    ) {
        if (isAccepted) {
            completeRequest.acceptCompleteRequest();
            fromCard.updateCardStatusToTradeComplete();
            toCard.updateCardStatusToTradeComplete();
        } else {
            completeRequest.refuseCompleteRequest();
        }
    }

    private void createCompleteRequestEvent(CompleteRequest completeRequest) {
        User receiver = completeRequest.getToCard().getUser();
        String message = completeRequest.createCompleteRequestMessage();
        applicationEventPublisher.publishEvent(new NotificationCreateEvent(
            receiver,
            completeRequest.getToCard(),
            message
        ));
    }

    private void createCompleteRequestDecisionEvent(CompleteRequest completeRequest, boolean isAccepted) {
        User receiver = completeRequest.getToCard().getUser();
        String message = completeRequest.createCompleteRequestDecisionMessage(isAccepted);
        applicationEventPublisher.publishEvent(new NotificationCreateEvent(
            receiver,
            completeRequest.getToCard(),
            message
        ));
    }
}

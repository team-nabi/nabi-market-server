package org.prgrms.nabimarketbe.domain.suggestion.service;

import org.prgrms.nabimarketbe.domain.card.entity.Card;
import org.prgrms.nabimarketbe.domain.card.repository.CardRepository;
import org.prgrms.nabimarketbe.domain.suggestion.dto.request.SuggestionRequestDTO;
import org.prgrms.nabimarketbe.domain.suggestion.dto.response.projection.SuggestionListReadPagingResponseDTO;
import org.prgrms.nabimarketbe.domain.suggestion.dto.response.SuggestionResponseDTO;
import org.prgrms.nabimarketbe.domain.suggestion.entity.DirectionType;
import org.prgrms.nabimarketbe.domain.suggestion.entity.Suggestion;
import org.prgrms.nabimarketbe.domain.suggestion.entity.SuggestionType;
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
public class SuggestionService {
    private final UserRepository userRepository;

    private final CheckService checkService;

    private final CardRepository cardRepository;

    private final SuggestionRepository suggestionRepository;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public SuggestionResponseDTO createSuggestion(
        String token,
        String suggestionType,
        SuggestionRequestDTO requestDto
    ) {
        User fromUser = userRepository.findById(checkService.parseToken(token))
            .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        Card fromCard = cardRepository.findByCardIdAndUser(requestDto.fromCardId(), fromUser)
            .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_MATCHED));

        Card toCard = cardRepository.findById(requestDto.toCardId())
            .orElseThrow(() -> new BaseException(ErrorCode.CARD_NOT_FOUND));

        if (isAuthorEquals(fromCard, toCard)) {
            throw new BaseException(ErrorCode.CARD_SUGGESTION_MYSELF_ERROR);
        }

        SuggestionType suggestionTypeEnum = SuggestionType.valueOf(suggestionType);

        if (!suggestionTypeEnum.isSuggestionAvailable(fromCard.getItem(), toCard.getItem())) {
            throw new BaseException(ErrorCode.SUGGESTION_TYPE_MISMATCH);
        }

        if (suggestionTypeEnum.equals(SuggestionType.POKE)) {
            pokeValidation(toCard);
        }

        Suggestion suggestion = Suggestion.builder()
            .suggestionType(suggestionTypeEnum)
            .fromCard(fromCard)
            .toCard(toCard)
            .build();

        User receiver = toCard.getUser();

        String message = suggestion.createSuggestionRequestMessage(fromUser);
        applicationEventPublisher.publishEvent(new NotificationCreateEvent(
            receiver,
            suggestion.getFromCard(),
            message
        ));

        Suggestion savedSuggestion = suggestionRepository.save(suggestion);

        return SuggestionResponseDTO.from(savedSuggestion);
    }

    @Transactional(readOnly = true)
    public SuggestionListReadPagingResponseDTO getSuggestionsByType(
        String token,
        DirectionType directionType,
        SuggestionType suggestionType,
        Long cardId,
        String cursorId,
        Integer size
    ) {
        Card card = cardRepository.findById(cardId)
            .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_MATCHED));

        if (!checkService.isEqual(token, card.getUser().getUserId())) {
            throw new BaseException(ErrorCode.USER_NOT_MATCHED);
        }

        return suggestionRepository.getSuggestionsByType(
            directionType,
            suggestionType,
            card.getCardId(),
            cursorId,
            size
        );
    }

    @Transactional
    public SuggestionResponseDTO updateSuggestionStatus(
        String token,
        Long fromCardId,
        Long toCardId,
        Boolean isAccepted
    ) {
        User toUser = userRepository.findById(checkService.parseToken(token))
            .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        Card fromCard = cardRepository.findById(fromCardId)
            .orElseThrow(() -> new BaseException(ErrorCode.CARD_NOT_FOUND));

        Card toCard = cardRepository.findByCardIdAndUser(toCardId, toUser)
            .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_MATCHED));

        Suggestion suggestion = suggestionRepository.findSuggestionByFromCardAndToCard(fromCard, toCard)
            .orElseThrow(() -> new BaseException(ErrorCode.SUGGESTION_NOT_FOUND));

        User receiver = fromCard.getUser();

        suggestion.decideSuggestion(isAccepted);

        String message = suggestion.createSuggestionDecisionMessage(isAccepted);
        applicationEventPublisher.publishEvent(new NotificationCreateEvent(
            receiver,
            suggestion.getFromCard(),
            message
        ));

        //TODO : 채팅방 생성

        return SuggestionResponseDTO.from(suggestion);
    }

    private boolean isAuthorEquals(
        Card fromCard,
        Card toCard
    ) {
        return fromCard.getUser().getUserId().equals(toCard.getUser().getUserId());
    }

    private void pokeValidation(Card toCard) {
        if (!toCard.isPokeAvailable()) {
            throw new BaseException(ErrorCode.SUGGESTION_TYPE_MISMATCH);
        }
    }
}

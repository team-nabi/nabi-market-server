package org.prgrms.nabimarketbe.domain.card.dto.request;

import org.prgrms.nabimarketbe.domain.card.entity.Card;
import org.prgrms.nabimarketbe.domain.card.entity.TradeType;
import org.prgrms.nabimarketbe.domain.category.entity.Category;
import org.prgrms.nabimarketbe.domain.category.entity.CategoryEnum;
import org.prgrms.nabimarketbe.domain.item.entity.Item;
import org.prgrms.nabimarketbe.domain.item.entity.PriceRange;
import org.prgrms.nabimarketbe.domain.user.entity.User;
import org.prgrms.nabimarketbe.global.annotation.ValidEnum;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

public record CardCreateRequestDTO(
        @NotBlank(message = "공백을 허용하지 않습니다.")
        String cardTitle,

        @NotBlank(message = "공백을 허용하지 않습니다.")
        String thumbNailImage,

        @NotBlank(message = "공백을 허용하지 않습니다.")
        String itemName,

        @ValidEnum(enumClass = PriceRange.class, message = "유효하지 않은 가격대입니다.")
        PriceRange priceRange,

        @ValidEnum(enumClass = TradeType.class, message = "유효하지 않은 거래 방식입니다.")
        TradeType tradeType,

        @NotBlank(message = "공백을 허용하지 않습니다.")
        String tradeArea,

        @ValidEnum(enumClass = CategoryEnum.class, message = "유효하지 않은 카테고리입니다.")
        CategoryEnum category,

        @NotNull(message = "비울 수 없는 값입니다.")
        Boolean pokeAvailable,

        @NotBlank(message = "공백을 허용하지 않습니다.")
        String content,

        List<CardImageCreateRequestDTO> images
) {
        public Item toItemEntity(Category category) {
                return Item.builder()
                        .itemName(itemName)
                        .priceRange(priceRange)
                        .category(category)
                        .build();
        }

        public Card toCardEntity(Item item, User user) {
                return Card.builder()
                        .cardTitle(cardTitle)
                        .thumbNailImage(thumbNailImage)
                        .content(content)
                        .tradeArea(tradeArea)
                        .poke(pokeAvailable)
                        .tradeType(tradeType)
                        .item(item)
                        .user(user)
                        .build();
        }
}

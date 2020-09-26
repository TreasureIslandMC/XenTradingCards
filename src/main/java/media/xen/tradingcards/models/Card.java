package media.xen.tradingcards.models;

import lombok.*;

public class Card {
    @Getter @Setter private CardRarity rarity;
    @Getter @Setter private CardType type;
    @Getter @Setter private String series;
    @Getter @Setter private boolean hasShinyVersion;
    @Getter @Setter private String info;
    @Getter @Setter private double buyPrice;
}

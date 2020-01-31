package info.fmro.shared.stream.objects;

import info.fmro.shared.entities.MarketDescription;

import java.io.Serializable;

public interface MarketCatalogueInterface
        extends Serializable {
    EventInterface getEventStump();

    String getMarketName();

    MarketDescription getDescription();
}

package info.fmro.shared.stream.objects;

import info.fmro.shared.entities.MarketDescription;
import info.fmro.shared.entities.RunnerCatalog;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

public interface MarketCatalogueInterface
        extends Serializable {
    String getMarketId();

    String getMarketName();

    Date getMarketStartTime();

    MarketDescription getDescription();

    List<RunnerCatalog> getRunners();

    EventInterface getEventStump();
}

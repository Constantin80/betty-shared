package info.fmro.shared.entities;

import info.fmro.shared.utility.Generic;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class MarketCatalogueTest {
    @Test
    void update()
            throws NoSuchFieldException {
        try {
            final MarketCatalogue firstObject = new MarketCatalogue("someId"), secondObject = new MarketCatalogue("someId");
            Generic.fillRandom(firstObject);
            Generic.fillRandom(secondObject);

            final String marketId = firstObject.getMarketId();
            final Field field = MarketCatalogue.class.getDeclaredField("marketId");
            field.setAccessible(true);
            field.set(secondObject, marketId);
            secondObject.setTimeStamp(firstObject.getTimeStamp() + 1L);

            firstObject.update(secondObject, new HashSet<>(1), Objects.requireNonNull(Generic.getMethod(MarketCatalogue.class, "getMarketId")));
            assertThat(firstObject).as("updated object different").isEqualToIgnoringGivenFields(secondObject);
        } catch (IllegalAccessException e) {
            fail("IllegalAccessException");
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            fail("InvocationTargetException");
            e.printStackTrace();
        } catch (InstantiationException e) {
            fail("InstantiationException");
            e.printStackTrace();
        }
    }
}

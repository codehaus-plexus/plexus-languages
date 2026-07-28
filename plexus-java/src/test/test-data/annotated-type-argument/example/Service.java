package example;

import java.util.List;

public interface Service<T extends @Nullable Object> {
    void accept(List<@Nullable String> values);
}

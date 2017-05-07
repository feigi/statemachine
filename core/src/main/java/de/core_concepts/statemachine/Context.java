package de.core_concepts.statemachine;


import org.apache.commons.lang.Validate;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Created by zieglerch on 26.11.2015.
 *
 * This class represents shared state between Actions during a state transition.
 *
 * @param <O> The object type residing on the Context on which the Actions operate.
 */
@Getter
@RequiredArgsConstructor
public class Context<O> {

  private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(Context.class);

  @NonNull
  private final O object;

  @Getter(AccessLevel.PACKAGE)
  private Map<LifecycleEvent, Object> lifecycleEventData = new HashMap<>();

  public Optional<Error> getValidationError() {
    return getDataFor(LifecycleEvent.VALIDATION_ERROR);
  }

  public boolean hasValidationError() {
    return lifecycleEventData.get(LifecycleEvent.VALIDATION_ERROR) != null;
  }

  void setDataFor(LifecycleEvent event, Object data) {
    if (data != null) {
      lifecycleEventData.put(event, data);
    }
  }

  /**
   * Sets data for the LifecycleEvent.EVENT_RECEIVED on the context. Be advised, this method overwrites data that may already been on the
   * context for this LifecycleEvent.
   *
   * @param data The data to set.
   */
  public void setEventData(Object data) {
    setDataFor(LifecycleEvent.EVENT_RECEIVED, data);
  }

  /**
   * @param event The LifecycleEvent for which the caller wants to get data for.
   * @param <T>   The type of the data object returned.
   * @return An Optional with the data of the given LifecycleEvent or Optional.empty() if there is none.
   */
  public <T> Optional<T> getDataFor(LifecycleEvent<T> event) {
    Validate.notNull(event);
    Object data = lifecycleEventData.get(event);
    return castData(event.getType(), data);
  }

  /**
   * Tries to cast <code>data</code> to <code>type</code> and returns an Optional
   *
   * @param type The type to cast to. Mandatory.
   * @param data The object to be cast. Optional.
   * @param <T>  The type to cast to.
   * @return If casting is successful an Optional containing the casted object, an empty Optional otherwise.
   */
  private <T> Optional<T> castData(Class<T> type, Object data) {
    try {
      return Optional.ofNullable(type.cast(data));
    } catch (ClassCastException e) {
      LOG.error(String.format("Failed to cast %s to type %s", data, type), e);
      return Optional.empty();
    }
  }

  /**
   * @param expectedClass The return type the caller expects.
   * @param <T>           The type the caller expects as return.
   * @return An Optional with the data of the most recent LifecycleEvent according to LifecycleEvent.ORDERED_LIFECYCLE_EVENTS or
   * Optional.empty() if no data at all, or of the given type was found.
   */
  public <T> Optional<T> getMostRecentData(Class<T> expectedClass) {
    // If no event data has been saved just bail
    if (!lifecycleEventData.isEmpty()) {

      // Iterate in reverse object to most recent data
      Iterator<LifecycleEvent<?>> descIterator = LifecycleEvent.ORDERED_LIFECYCLE_EVENTS.descendingIterator();
      while (descIterator.hasNext()) {
        LifecycleEvent event = descIterator.next();
        Object data = lifecycleEventData.get(event);
        if (data != null) {
          return castData(expectedClass, data);
        }
      }
    }
    return Optional.empty();
  }

  /**
   * @return An Optional with the data of the most recent LifecycleEvent
   */
  public Optional<Object> getMostRecentData() {
    return getMostRecentData(Object.class);
  }

  /**
   * Clears all data set in the course of a state transition, so that no data spills over to a following state transition.
   */
  void clearData() {
    lifecycleEventData.clear();
  }
}

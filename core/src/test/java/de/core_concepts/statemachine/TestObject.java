package de.core_concepts.statemachine;


/**
 * Context object for AbstractStatemachineTest
 * <p>
 * Created by zieglerch on 17.06.2016.
 */
public class TestObject {

    TestState currentState;

    public TestObject() {
    }

    public TestState getCurrentState() {
        return this.currentState;
    }

    public void setCurrentState(TestState currentState) {
        this.currentState = currentState;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof TestObject)) return false;
        final TestObject other = (TestObject) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$currentState = this.getCurrentState();
        final Object other$currentState = other.getCurrentState();
        if (this$currentState == null ? other$currentState != null : !this$currentState.equals(other$currentState))
            return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $currentState = this.getCurrentState();
        result = result * PRIME + ($currentState == null ? 43 : $currentState.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof TestObject;
    }

    public String toString() {
        return "TestObject(currentState=" + this.getCurrentState() + ")";
    }
}

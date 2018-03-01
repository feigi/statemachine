package de.core_concepts.statemachine.spring;

/**
 *
 */
public class TestObject {

    private TestState state;

    public TestObject() {
    }

    public TestState getState() {
        return this.state;
    }

    public void setState(TestState state) {
        this.state = state;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof TestObject)) return false;
        final TestObject other = (TestObject) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$state = this.getState();
        final Object other$state = other.getState();
        if (this$state == null ? other$state != null : !this$state.equals(other$state)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $state = this.getState();
        result = result * PRIME + ($state == null ? 43 : $state.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof TestObject;
    }

    public String toString() {
        return "TestObject(state=" + this.getState() + ")";
    }
}

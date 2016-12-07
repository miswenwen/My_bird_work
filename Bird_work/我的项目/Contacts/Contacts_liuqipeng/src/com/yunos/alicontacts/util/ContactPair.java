package com.yunos.alicontacts.util;

import libcore.util.Objects;

public class ContactPair<F, S> {
    public F first;
    public S second;

    /**
     * Constructor for a ContactPair.
     *
     * @param first the first object in the ContactPair
     * @param second the second object in the ContactPair
     */
    public ContactPair(F first, S second) {
        this.first = first;
        this.second = second;
    }

    /**
     * Checks the two objects for equality by delegating to their respective
     * {@link Object#equals(Object)} methods.
     *
     * @param o the {@link ContactPair} to which this one is to be checked for equality
     * @return true if the underlying objects of the ContactPair are both considered
     *         equal
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ContactPair)) {
            return false;
        }
        ContactPair<?, ?> p = (ContactPair<?, ?>) o;
        return Objects.equal(p.first, first) && Objects.equal(p.second, second);
    }

    /**
     * Compute a hash code using the hash codes of the underlying objects
     *
     * @return a hashcode of the ContactPair
     */
    @Override
    public int hashCode() {
        return (first == null ? 0 : first.hashCode()) ^ (second == null ? 0 : second.hashCode());
    }

    /**
     * Convenience method for creating an appropriately typed ContactPair.
     * @param a the first object in the ContactPair
     * @param b the second object in the ContactPair
     * @return a ContactPair that is templatized with the types of a and b
     */
    public static <A, B> ContactPair <A, B> create(A a, B b) {
        return new ContactPair<A, B>(a, b);
    }
}

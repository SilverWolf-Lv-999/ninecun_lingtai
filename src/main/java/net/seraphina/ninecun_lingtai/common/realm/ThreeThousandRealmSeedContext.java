package net.seraphina.ninecun_lingtai.common.realm;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.OptionalLong;

public final class ThreeThousandRealmSeedContext {
    private static final ThreadLocal<Deque<Long>> REALM_SEED_STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private ThreeThousandRealmSeedContext() {
    }

    public static RealmSeedScope push(long realmSeed) {
        REALM_SEED_STACK.get().push(realmSeed);
        return new RealmSeedScope(realmSeed);
    }

    public static OptionalLong getCurrentSeed() {
        Deque<Long> realmSeeds = REALM_SEED_STACK.get();
        if (realmSeeds.isEmpty()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(realmSeeds.peek());
    }

    public static final class RealmSeedScope implements AutoCloseable {
        private final long thisRealmSeed;
        private boolean thisClosed;

        private RealmSeedScope(long realmSeed) {
            this.thisRealmSeed = realmSeed;
        }

        @Override
        public void close() {
            if (this.thisClosed) {
                return;
            }

            Deque<Long> realmSeeds = REALM_SEED_STACK.get();
            if (realmSeeds.isEmpty() || realmSeeds.peek() != this.thisRealmSeed) {
                throw new IllegalStateException("Three-thousand-realm seed scopes must close in stack order");
            }

            realmSeeds.pop();
            if (realmSeeds.isEmpty()) {
                REALM_SEED_STACK.remove();
            }
            this.thisClosed = true;
        }
    }
}

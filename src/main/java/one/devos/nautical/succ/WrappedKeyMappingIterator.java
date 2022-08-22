package one.devos.nautical.succ;

import java.util.Iterator;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.AbstractIterator;

import net.minecraft.client.KeyMapping;

public class WrappedKeyMappingIterator extends AbstractIterator<KeyMapping> {
	public final Iterator<KeyMapping> wrapped;

	public WrappedKeyMappingIterator(Iterator<KeyMapping> wrapped) {
		this.wrapped = wrapped;
	}

	@Nullable
	@Override
	protected KeyMapping computeNext() {
		if (wrapped.hasNext()) {
			KeyMapping next = wrapped.next();
			for (KeyMapping climbKey : SuccKeybinds.CLIMBING_KEYS) {
				if (next == climbKey) {
					return computeNext();
				}
			}
			return next;
		}
		return endOfData();
	}
}

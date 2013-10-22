/**
 * Copyright (C) 2010 - 2013 SCAPE Consortium <office@scape-project.eu>
 *
 * This file is part of Bitwiser.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * 
 */
package eu.scape_project.bitwiser.utils;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 */
public class XORShiftRandom extends Random {
	/** */
	private static final long serialVersionUID = -96188754726659250L;

	private AtomicLong seed = new AtomicLong(System.nanoTime());

	public XORShiftRandom() {
	}
	public XORShiftRandom( long seed ) {
		this.seed.set( seed );
	}

	protected synchronized int next(int nbits) {
		// N.B. Not thread-safe?
		long x = this.seed.get();
		x ^= (x << 21);
		x ^= (x >>> 35);
		x ^= (x << 4);
		this.seed.set(x);
		x &= ((1L << nbits) -1);
		return (int) x;
	}
}

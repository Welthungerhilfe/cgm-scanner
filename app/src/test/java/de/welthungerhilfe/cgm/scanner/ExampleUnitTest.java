package de.welthungerhilfe.cgm.scanner;

import android.arch.core.executor.testing.InstantTaskExecutorRule;
import android.content.Context;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import de.welthungerhilfe.cgm.scanner.datasource.repository.PersonRepository;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }
}
/**
 * 
 */
package com.github.podd.impl.file.test;

import com.github.podd.api.PoddRepositoryManager;
import com.github.podd.api.file.PoddFileRepositoryManager;
import com.github.podd.api.file.test.AbstractPoddFileRepositoryManagerTest;
import com.github.podd.impl.PoddRepositoryManagerImpl;
import com.github.podd.impl.file.PoddFileRepositoryManagerImpl;

/**
 * @author kutila
 */
public class PoddFileRepositoryManagerImplTest extends AbstractPoddFileRepositoryManagerTest
{
    
    @Override
    protected PoddFileRepositoryManager getNewPoddFileRepositoryManager()
    {
        return new PoddFileRepositoryManagerImpl();
    }
    
    @Override
    protected PoddRepositoryManager getNewPoddRepositoryManager()
    {
        return new PoddRepositoryManagerImpl();
    }
    
}

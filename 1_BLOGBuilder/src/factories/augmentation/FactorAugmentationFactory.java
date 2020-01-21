package factories.augmentation;

import javax.xml.parsers.FactoryConfigurationError;

import blogbuilder.World;
import elements.Factor;
import factories.ElementFactory;

public class FactorAugmentationFactory extends GenericAugmentationFactory {
	
	public FactorAugmentationFactory(World baseWorld) {
		super(baseWorld);
	}

	@Override
	public void insertFactors(World w) {
		// 1. Inherit factors from baseworld (default behavior) in init-method
		
		// 2. Add another factor (using the base world's base factory).
		ElementFactory ff = super.baseWorld.getFactorFactory();
		ff.insertFactors(w);
	}

	@Override
	public ElementFactory getBaseFactory() {
		return super.baseWorld.getFactorFactory();
	}

	
}

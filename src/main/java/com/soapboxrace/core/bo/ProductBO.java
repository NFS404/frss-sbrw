package com.soapboxrace.core.bo;

import com.soapboxrace.core.dao.CategoryDAO;
import com.soapboxrace.core.dao.PersonaDAO;
import com.soapboxrace.core.dao.ProductDAO;
import com.soapboxrace.core.dao.VinylProductDAO;
import com.soapboxrace.core.jpa.CategoryEntity;
import com.soapboxrace.core.jpa.PersonaEntity;
import com.soapboxrace.core.jpa.ProductEntity;
import com.soapboxrace.core.jpa.VinylProductEntity;
import com.soapboxrace.jaxb.http.ArrayOfProductTrans;
import com.soapboxrace.jaxb.http.ProductTrans;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.util.List;

@Stateless
public class ProductBO {

	@EJB
	private ProductDAO ProductDAO;

	@EJB
	private CategoryDAO categoryDao;

	@EJB
	private VinylProductDAO vinylProductDao;

	@EJB
	private PersonaDAO personaDao;

	public List<ProductEntity> productsInCategory(String categoryName, String productType, Long personaId) {
		boolean premium = false;
		boolean admin = false;
		int level = 1;
		if (personaId != null && !personaId.equals(0L)) {
			PersonaEntity personaEntity = personaDao.findById(personaId);
			premium = personaEntity.getUser().isPremium();
			admin = personaEntity.getUser().isAdmin();
			level = personaEntity.getLevel();
		}
		return ProductDAO.findByLevelEnabled(categoryName, productType, level, true, premium, admin);
	}

	public List<CategoryEntity> categories() {
		return categoryDao.getAll();
	}

	public ArrayOfProductTrans getVinylByCategory(CategoryEntity categoryEntity, Long personaId) {
		boolean premium = false;
		int level = 1;
		if (personaId != null && !personaId.equals(0L)) {
			PersonaEntity personaEntity = personaDao.findById(personaId);
			premium = personaEntity.getUser().isPremium();
			level = personaEntity.getLevel();
		}
		ArrayOfProductTrans arrayOfProductTrans = new ArrayOfProductTrans();
		List<VinylProductEntity> vinylProductEntity = vinylProductDao.findByCategoryLevelEnabled(categoryEntity, level, true, premium);
		for (VinylProductEntity entity : vinylProductEntity) {
			ProductTrans productTrans = new ProductTrans();
			productTrans.setCurrency(entity.getCurrency());
			productTrans.setDurationMinute(entity.getDurationMinute());
			productTrans.setHash(entity.getHash());
			productTrans.setIcon(entity.getIcon());
			productTrans.setSecondaryIcon(entity.getSecondaryIcon());
			productTrans.setLevel(entity.getLevel());
			productTrans.setPrice(entity.getPrice());
			productTrans.setPriority(entity.getPriority());
			productTrans.setProductId(entity.getProductId());
			productTrans.setProductTitle(entity.getProductTitle());
			productTrans.setProductType(entity.getProductType());
			productTrans.setUseCount(entity.getUseCount());
			arrayOfProductTrans.getProductTrans().add(productTrans);
		}
		return arrayOfProductTrans;
	}

	
}

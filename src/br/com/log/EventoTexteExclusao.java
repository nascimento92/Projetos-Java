package br.com.log;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;

public class EventoTexteExclusao implements EventoProgramavelJava {

	@Override
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		start(arg0);
		
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub	
	}
	
	public void start (PersistenceEvent arg0) {
		try {
			
			DynamicVO VO = (DynamicVO) arg0.getVo();
			String instancia = VO.getValueObjectID();
			int qtdStrings = instancia.indexOf(".");
			String instanciaNew = instancia.substring(0,qtdStrings);
			String tabela = descobreAhTabela(instanciaNew);
			Object primaryKey = VO.getPrimaryKey();
			
			String verificaCampos = verificaCampos(tabela,VO);
			
			System.out.println("************************** CAMPOS: "+verificaCampos);
			
			if(verificaCampos!="") {
				BigDecimal usuario = getUsuLogado();
				
				System.out.println("************************** \n"
						+ "TABELA: "+tabela
						+ "USUARIO: "+usuario
						+ "CAMPOS: "+verificaCampos
						+ "PRIMARY KEY"+primaryKey);
				
				registrarDadosExcluidos(tabela,usuario,verificaCampos,primaryKey);
			}
						
		} catch (Exception e) {
			System.out.println("************************** [EventoTesteExclusao] - Erro! "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	private String descobreAhTabela(String instancia) throws Exception{
		
		JapeWrapper DAO = JapeFactory.dao("Instancia");
		DynamicVO VO = DAO.findOne("NOMEINSTANCIA=?",new Object[] { instancia });
		
		String tabela = VO.asString("NOMETAB");
		
		return tabela;
	}
	
	private String verificaCampos (String tabela, DynamicVO VO) throws Exception {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> parceiro = dwfEntityFacade
				.findByDynamicFinder(new FinderWrapper("AD_CAMPOSLOG", "this.NOMETAB = ? ", new Object[] { tabela }));
		
		String campos="";

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			String campo = DynamicVO.asString("NOMECAMPO");
			String tipo = DynamicVO.asString("TIPO");
			
			if("E".equals(tipo) || "AE".equals(tipo)) {
				Object valor = VO.getProperty(campo);		
				campos = campos+campo+":"+valor+";";
			}
		}
		
		return campos;
	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal codUsuLogado = BigDecimal.ZERO;
	    codUsuLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
	    return codUsuLogado;    	
	}
	
	private void registrarDadosExcluidos(String tabela, BigDecimal usuario, String dadosExcluidos, Object pk) throws Exception{
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_LOG");
			DynamicVO VO = (DynamicVO) NPVO;
			
			String stringPK = pk.toString();
			String novaPK = stringPK.substring(stringPK.indexOf("[")+1,stringPK.lastIndexOf("]"));
				
			VO.setProperty("TABELA", tabela);
			VO.setProperty("CAMPO", "EXCLUIDOS");
			VO.setProperty("VLROLD", "");
			VO.setProperty("VLRNEW", "");
			VO.setProperty("CODUSU", usuario);
			VO.setProperty("DTALTER", new Timestamp(System.currentTimeMillis()));
			VO.setProperty("PKTABELA", novaPK);
			VO.setProperty("EXCLUSAO", dadosExcluidos);
			
			dwfFacade.createEntity("AD_LOG", (EntityVO) VO);
			
		} catch (Exception e) {
			System.out.println("** [EventoTesteExclusao] - NAO FOI POSSIVEL SALVAR! "+e.getMessage());
			e.printStackTrace();
		}
	}

}

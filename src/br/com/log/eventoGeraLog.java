package br.com.log;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
//import java.util.ArrayList;
//import java.util.Collection;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.eventoprogramavel.EventoProgramavelJava;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.event.PersistenceEvent;
import br.com.sankhya.jape.event.TransactionContext;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.auth.AuthenticationInfo;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;
import br.com.sankhya.ws.ServiceContext;


public class eventoGeraLog implements EventoProgramavelJava {
	
	String tipo="";
	
	public void afterDelete(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
		tipo = "insert";
		start(arg0,tipo);
	}

	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeCommit(TransactionContext arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		tipo = "delete";
		start(arg0,tipo);
	}

	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		
	}

	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		tipo = "alteracao";
		start(arg0,tipo);
	}
	
	private void start(PersistenceEvent arg0, String tipo){
		try {
			
			DynamicVO VO = (DynamicVO) arg0.getVo();

			String instancia = VO.getValueObjectID();
			int qtdStrings = instancia.indexOf(".");
			String instanciaNew = instancia.substring(0,qtdStrings);
			String tabela = descobreAhTabela(instanciaNew);
			Object primaryKey = VO.getPrimaryKey();
			
			if("alteracao".equals(tipo)) {
				pegaOsCamposDaTabela(tabela, arg0, primaryKey);
			}
			else if ("delete".equals(tipo)) {
				String valoresDosCamposDeletados = verificaCamposDelete(tabela,VO);
				
				if(valoresDosCamposDeletados!="") {
					BigDecimal usuario = getUsuLogado();
					if(usuario==null) {
						usuario = new BigDecimal(0);
					}
					registrarDadosExcluidos(tabela,usuario,valoresDosCamposDeletados,primaryKey);
				}
			}else if ("insert".equals(tipo)) {
				String vericaCamposInclusao = verificaCamposInsert(tabela,VO);
				
				if(vericaCamposInclusao!="") {
					BigDecimal usuario = getUsuLogado();
					if(usuario==null) {
						usuario = new BigDecimal(0);
					}

					registrarDadosIncluidos(tabela,usuario,vericaCamposInclusao,primaryKey);
				}
			}
			
			
		} catch (Exception e) {
			System.out.println("**[eventoGeraLog] Deu erro!" + e.getMessage());
			e.printStackTrace();
		}		

	}
	
	private String descobreAhTabela(String instancia) throws Exception{
		
		JapeWrapper DAO = JapeFactory.dao("Instancia");
		DynamicVO VO = DAO.findOne("NOMEINSTANCIA=?",new Object[] { instancia });
		
		String tabela = VO.asString("NOMETAB");
		
		return tabela;
	}
	
	private void pegaOsCamposDaTabela(String tabela, PersistenceEvent arg0, Object pk) throws Exception{
		
		//Collection<String> campos = new ArrayList();
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();

			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT NOMECAMPO FROM TDDCAM WHERE NOMETAB='"+tabela+"'");
			contagem = nativeSql.executeQuery();

			while (contagem.next()) {
				
				if(arg0.getModifingFields().isModifing(contagem.getString(1))){
					
					if(verificaSeOhCampoGeraLog(tabela,contagem.getString(1))){
						
						String campo = contagem.getString(1);
						Object oldValue = arg0.getModifingFields().getOldValue(contagem.getString(1));
						Object newValue = arg0.getModifingFields().getNewValue(contagem.getString(1));
						BigDecimal usuLogado = getUsuLogado();
						
						salvaAlteracoes(tabela,campo,oldValue,newValue,usuLogado,pk);
					}
					
				}
			}
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	}
	
	private BigDecimal getUsuLogado() {
		BigDecimal usuario = BigDecimal.ZERO;
		try {
			
		    BigDecimal codUsuLogado = ((AuthenticationInfo)ServiceContext.getCurrent().getAutentication()).getUserID();
		    if(codUsuLogado!=null) {
		    	usuario = codUsuLogado;
		    }
			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
	    return usuario;    	
	}
	
	private boolean verificaSeOhCampoGeraLog(String tabela, Object campo) throws Exception{
		
		boolean valida = false;
		
		JapeWrapper DAO = JapeFactory.dao("AD_CAMPOSLOG");
		DynamicVO VO = DAO.findOne("NOMETAB=? AND NOMECAMPO=?",new Object[] { tabela,campo });
		
		if(VO!=null){
			return valida=true;
		}
		
		return valida;

	}
	
	private void salvaAlteracoes(String tabela, Object campo, Object vlrold, Object vlrnew, BigDecimal usuario, Object pk) throws Exception{
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_LOG");
			DynamicVO VO = (DynamicVO) NPVO;
			
			String stringPK = pk.toString();
			String novaPK = stringPK.substring(stringPK.indexOf("[")+1,stringPK.lastIndexOf("]"));
			
			if(vlrnew==null) {
				vlrnew="Vazio";
			}
			
			if(vlrold==null) {
				vlrold="Vazio";
			}
			
			VO.setProperty("TABELA", tabela);
			VO.setProperty("CAMPO", campo.toString());
			VO.setProperty("VLROLD", vlrold.toString());
			VO.setProperty("VLRNEW", vlrnew.toString());
			VO.setProperty("CODUSU", usuario);
			VO.setProperty("DTALTER", new Timestamp(System.currentTimeMillis()));
			VO.setProperty("PKTABELA", novaPK);
			
			dwfFacade.createEntity("AD_LOG", (EntityVO) VO);
			
		} catch (Exception e) {
			System.out.println("** [br.com.log.eventoGeraLog] - NAO FOI POSSIVEL SALVAR DADOS ALTERADOS! "+e.getMessage()+"\n"+e.getCause());
			e.printStackTrace();
		}
	
	}
	
	private String verificaCamposDelete(String tabela, DynamicVO VO) throws Exception {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> parceiro = dwfEntityFacade
				.findByDynamicFinder(new FinderWrapper("AD_CAMPOSLOG", "this.NOMETAB = ? ", new Object[] { tabela }));
		
		String campos="";

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			String campo = DynamicVO.asString("NOMECAMPO");
			String tipo = DynamicVO.asString("TIPO");
			
			if(tipo.contains("E")) {
				Object valor = VO.getProperty(campo);
				if(valor!=null) {
					campos = campos+campo+":"+valor+";";
				}	
			}
		}
		
		return campos;
	}
	
	private String verificaCamposInsert(String tabela, DynamicVO VO) throws Exception {
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();

		Collection<?> parceiro = dwfEntityFacade
				.findByDynamicFinder(new FinderWrapper("AD_CAMPOSLOG", "this.NOMETAB = ? ", new Object[] { tabela }));
		
		String campos="";

		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			String campo = DynamicVO.asString("NOMECAMPO");
			String tipo = DynamicVO.asString("TIPO");
			
			if(tipo.contains("I")) {
				Object valor = VO.getProperty(campo);
				if(valor!=null) {
					campos = campos+campo+":"+valor+";";
				}	
			}
		}
		
		return campos;
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
			System.out.println("** [br.com.log.eventoGeraLog] - NAO FOI POSSIVEL SALVAR DADOS EXCLUIDOS! "+e.getMessage()+"\n"+e.getCause());
			e.printStackTrace();
		}
	}
	
	private void registrarDadosIncluidos(String tabela, BigDecimal usuario, String dadosIncluidos, Object pk) throws Exception{
		
		try {
			
			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_LOG");
			DynamicVO VO = (DynamicVO) NPVO;
			
			String stringPK = pk.toString();
			String novaPK = stringPK.substring(stringPK.indexOf("[")+1,stringPK.lastIndexOf("]"));
					
			VO.setProperty("TABELA", tabela);
			VO.setProperty("CAMPO", "INCLUSÃO");
			VO.setProperty("VLROLD", "");
			VO.setProperty("VLRNEW", "");
			VO.setProperty("CODUSU", usuario);
			VO.setProperty("DTALTER", new Timestamp(System.currentTimeMillis()));
			VO.setProperty("PKTABELA", novaPK);
			VO.setProperty("INCLUSAO", dadosIncluidos);
			
			dwfFacade.createEntity("AD_LOG", (EntityVO) VO);
			
		} catch (Exception e) {
			System.out.println("** [br.com.log.eventoGeraLog] - NAO FOI POSSIVEL SALVAR DADOS INCLUIDOS! "+e.getMessage()+"\n"+e.getCause());
			e.printStackTrace();
		}
		
	
	}
	
}

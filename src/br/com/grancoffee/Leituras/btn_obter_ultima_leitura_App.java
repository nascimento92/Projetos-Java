package br.com.grancoffee.Leituras;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;

import br.com.sankhya.extensions.actionbutton.AcaoRotinaJava;
import br.com.sankhya.extensions.actionbutton.ContextoAcao;
import br.com.sankhya.extensions.actionbutton.Registro;
import br.com.sankhya.jape.EntityFacade;
import br.com.sankhya.jape.bmp.PersistentLocalEntity;
import br.com.sankhya.jape.dao.JdbcWrapper;
import br.com.sankhya.jape.sql.NativeSql;
import br.com.sankhya.jape.util.FinderWrapper;
import br.com.sankhya.jape.vo.DynamicVO;
import br.com.sankhya.jape.vo.EntityVO;
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class btn_obter_ultima_leitura_App implements AcaoRotinaJava {
	String retorno="";
	@Override
	public void doAction(ContextoAcao arg0) throws Exception {
		Registro[] linhas = arg0.getLinhas();
		int length = linhas.length;
		for(int i=0;i<length;i++) {
			start(linhas[i]);
		}	
		
		arg0.setMensagemRetorno(retorno);
		
	}
	
	public void start(Registro linhas) throws Exception {
		String patrimonio = (String) linhas.getCampo("CODBEM");
		BigDecimal numeroLeitura = (BigDecimal) linhas.getCampo("NRLEITURA");
		String oficial = (String) linhas.getCampo("OFICIAL");
		
		if(oficial.equals("S")) {
			retorno = retorno+"\n<br>Patrimonio: "+patrimonio+" Leitura Oficial, não pode ser alterada!\n";
		}else {
			Object dataUltimaLeitura = getDataUltimaLeitura(patrimonio);
			if(dataUltimaLeitura!=null) {
				salvaDados(numeroLeitura,patrimonio,dataUltimaLeitura);
				BigDecimal codabastecedor = getAbastecedor(dataUltimaLeitura,patrimonio);
				if(codabastecedor!=null) {
					linhas.setCampo("CODUSU", codabastecedor);
				}
				linhas.setCampo("LEITURAAPP", "S");
			}else {
				retorno = retorno+"\n<br>Patrimonio: "+patrimonio+" Leitura não localizada!\n";
			}
		}
	}
	
	public Object getDataUltimaLeitura(String patrimonio) {
		Object data = null;
		
		try {
			
			JdbcWrapper jdbcWrapper = null;
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
			ResultSet contagem;
			NativeSql nativeSql = new NativeSql(jdbcWrapper);
			nativeSql.resetSqlBuf();
			nativeSql.appendSql("SELECT MAX(DTLEITURA) AS DT FROM AD_LEITURASAPP WHERE CODBEM='"+patrimonio+"'");
			contagem = nativeSql.executeQuery();
			while (contagem.next()) {
				data = contagem.getObject("DT");
			}
			
		} catch (Exception e) {
			System.out.println("** [btn_obter_ultima_leitura_App] NAO FOI POSSIVEL LOCALIZAR A ULTIMA DATA **"+e.getMessage());
			e.printStackTrace();
		}
		return data;
		
	}
	
	public void salvaDados(BigDecimal numeroLeitura, String patrimonio, Object dataUltimaLeitura) {
		try {
			
			EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
			Collection<?> parceiro = dwfEntityFacade.findByDynamicFinder(new FinderWrapper("iteleitura","this.NRLEITURA=? AND this.CODBEM=?", new Object[] { numeroLeitura,patrimonio }));
			for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {
			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			EntityVO NVO = (EntityVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);
			DynamicVO VO = (DynamicVO) NVO;
			
			BigDecimal tecla = VO.asBigDecimal("TECLA");
			BigDecimal leitura = getDadosApp(dataUltimaLeitura,patrimonio,tecla);
			VO.setProperty("LEITURAATU",leitura);
			itemEntity.setValueObject(NVO);
			}
			
			retorno = retorno+"\n<br>Patrimonio: "+patrimonio+" Atualizado!\n";
		} catch (Exception e) {
			System.out.println("** [btn_obter_ultima_leitura_App] NAO FOI POSSIVEL SALVAR OS DADOS DA ULTIMA LEITURA **"+e.getMessage());
			e.printStackTrace();
		}
	}
	
	public BigDecimal getDadosApp(Object dataUltimaLeitura, String patrimonio, BigDecimal Tecla) throws Exception {	
		BigDecimal leitura = null;
		JapeWrapper DAO = JapeFactory.dao("LeiturasAPP");
		DynamicVO VO = DAO.findOne("CODBEM=? AND DTLEITURA=? AND TECLA=?",new Object[] { patrimonio,dataUltimaLeitura,Tecla});
		if(VO!=null) {
			leitura = VO.asBigDecimal("LEITURAATUAL");
		}else {
			leitura=new BigDecimal(0);
		}
		
		return leitura;
	}
	
	public BigDecimal getAbastecedor(Object dataUltimaLeitura, String patrimonio) throws Exception {
		BigDecimal leitura = null;
		JapeWrapper DAO = JapeFactory.dao("LeiturasAPP");
		DynamicVO VO = DAO.findOne("CODBEM=? AND DTLEITURA=?",new Object[] { patrimonio,dataUltimaLeitura});
		if(VO!=null) {
			leitura=VO.asBigDecimal("CODABAST");
		}
		return leitura;
	}

}


package br.com.TGFITE;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.Format;
import java.text.SimpleDateFormat;
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
import br.com.sankhya.jape.wrapper.JapeFactory;
import br.com.sankhya.jape.wrapper.JapeWrapper;
import br.com.sankhya.modelcore.util.EntityFacadeFactory;

public class tgfite_evento_desconto_programado implements EventoProgramavelJava {

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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		start(arg0);		
	}

	@Override
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		start(arg0);
		
	}
	
	//1.0
	private void start(PersistenceEvent arg0) throws Exception {
		DynamicVO VO = (DynamicVO) arg0.getVo();
		BigDecimal numeroUnico = VO.asBigDecimal("NUNOTA");
		BigDecimal produto = VO.asBigDecimal("CODPROD");
		Format formatMes = new SimpleDateFormat("MM");
		Format formatAno = new SimpleDateFormat("YYYY");
		
		if(produto.intValue()==8) {
			DynamicVO tgfcab = tgfcab(numeroUnico);
			BigDecimal top = tgfcab.asBigDecimal("CODTIPOPER");
			String verificaSeEhUmaTopDeLocacao = verificaSeEhUmaTopDeLocacao(top);
					
				if("S".equals(verificaSeEhUmaTopDeLocacao))	{
					BigDecimal contrato = tgfcab.asBigDecimal("NUMCONTRATO");
					Timestamp dataEntrada = tgfcab.asTimestamp("DTENTSAI");
					String mes = formatMes.format(dataEntrada);
					String ano = formatAno.format(dataEntrada);
					
					BigDecimal valorDesconto = null;
					
					valorDesconto = verificaDescontoProgramado(contrato,mes,ano);
					
					if(valorDesconto!=null) {
						VO.setProperty("VLRDESC", valorDesconto);
						System.out.println(" ## DESCONTO PROGRAMADO ## \n NUNOTA: "+numeroUnico+
								"\nTOP: "+top+
								"\nTOP DE LOCACAO: "+verificaSeEhUmaTopDeLocacao+
								"\nCONTRATO: "+contrato+
								"\nDATA ENTRADA: "+dataEntrada+
								"\nMES: "+mes+
								"\nANO: "+ano+
								"\nVLR DESCONTO: "+valorDesconto);
				}

			}
		}
	}
	
	//1.1
	private DynamicVO tgfcab(BigDecimal nunota) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("CabecalhoNota");
		DynamicVO VO = DAO.findOne("NUNOTA=?",new Object[] { nunota });
		return VO;
	}
	
	//1.2
	private String verificaSeEhUmaTopDeLocacao(BigDecimal top) throws Exception {
		String locacao = "N";
		JdbcWrapper jdbcWrapper = null;
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		jdbcWrapper = dwfEntityFacade.getJdbcWrapper();
		ResultSet contagem;
		NativeSql nativeSql = new NativeSql(jdbcWrapper);
		nativeSql.resetSqlBuf();
		nativeSql.appendSql("SELECT MAX(AD_TOPLOC) AS LOCACAO FROM TGFTOP WHERE CODTIPOPER="+top);
		contagem = nativeSql.executeQuery();

		while (contagem.next()) {
			locacao = contagem.getString("LOCACAO");
		}	
		return locacao;
	}
	
	//1.3
	private BigDecimal verificaDescontoProgramado(BigDecimal numcontrato, String mes, String ano) throws Exception {
		String referencia1 = "01/"+mes+ano;	
		String referencia2 = null;	
		
		if("01".equals(mes)||"03".equals(mes)||"05".equals(mes)||"07".equals(mes)||"08".equals(mes)||"10".equals(mes)||"12".equals(mes)) {
			referencia2 = "31/"+mes+ano;
		}else if ("04".equals(mes)||"06".equals(mes)||"09".equals(mes)||"11".equals(mes)) {
			referencia2 = "30/"+mes+ano;
		}else if ("02".equals(mes)) {
			referencia2 = "28/"+mes+ano;
		}
		
		BigDecimal valorDesconto = BigDecimal.ZERO;
		
		EntityFacade dwfEntityFacade = EntityFacadeFactory.getDWFFacade();
		Collection<?> parceiro = dwfEntityFacade
				.findByDynamicFinder(new FinderWrapper("AD_DESCONTOFRANQUIA", "this.REFERENCIA >=? AND this.REFERENCIA <=? AND this.NUMCONTRATO=? ", new Object[] { referencia1,referencia2,numcontrato }));
		for (Iterator<?> Iterator = parceiro.iterator(); Iterator.hasNext();) {

			PersistentLocalEntity itemEntity = (PersistentLocalEntity) Iterator.next();
			DynamicVO DynamicVO = (DynamicVO) ((DynamicVO) itemEntity.getValueObject()).wrapInterface(DynamicVO.class);

			if(DynamicVO!=null) {
				valorDesconto = DynamicVO.asBigDecimal("VLRDESC");
			}
		}
		
		return valorDesconto;

	}
}

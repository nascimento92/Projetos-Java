package br.com.TGFITE;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import com.sankhya.util.TimeUtils;

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


public class eventoContratoDataFixa implements EventoProgramavelJava {
	private Timestamp dhvenc;
	
	public void setDhVenc(Timestamp dh) {
		this.dhvenc = dh;
		//gg
	}

	public Timestamp getDhVenc() {
		return this.dhvenc;
	}
	
	
	public void afterDelete(PersistenceEvent arg0) throws Exception {

	}

	public void afterInsert(PersistenceEvent arg0) throws Exception {
	}

	
	public void afterUpdate(PersistenceEvent arg0) throws Exception {
		
	}

	
	public void beforeCommit(TransactionContext arg0) throws Exception {
		
	}

	
	public void beforeDelete(PersistenceEvent arg0) throws Exception {
		
	}

	
	public void beforeUpdate(PersistenceEvent arg0) throws Exception {
		
	}


	
	public void beforeInsert(PersistenceEvent arg0) throws Exception {
		EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
		DynamicVO financeiroVO = (DynamicVO) arg0.getVo();
		BigDecimal numContrato =  financeiroVO.asBigDecimalOrZero("NUMCONTRATO");
		BigDecimal valorBoleto = financeiroVO.asBigDecimal("VLRDESDOB");

		if (validaFinanceiro(financeiroVO)) {
			Date dataVencimento = financeiroVO.asTimestamp("DTVENC");
			DynamicVO ContratoVO = (DynamicVO) dwfFacade.findEntityByPrimaryKeyAsVO("Contrato",numContrato);

			if (!validaContrato(ContratoVO)) {
				return;
			}

			Timestamp dataReferencia = getNewData(ContratoVO,dataVencimento);	

			if(dataReferencia.compareTo(dataVencimento)!=0) {
				financeiroVO.setProperty("DTVENC",dataReferencia );
			}
			
			//implementação Gabriel.Nascimento 16/02/21 Funcionalidade para alterar o valor do boleto.
			
			BigDecimal top = financeiroVO.asBigDecimal("CODTIPOPER");
			if("S".equals(verificaSeEhUmaTopDeLocacao(top))) {
				
				Timestamp dtAtual = TimeUtils.getNow();
				Format formatMes = new SimpleDateFormat("MM");
				Format formatAno = new SimpleDateFormat("YYYY");
				String mes = formatMes.format(dtAtual);
				String ano = formatAno.format(dtAtual);
				BigDecimal valorDesconto = verificaDescontoProgramado(numContrato,mes,ano);
				
				if(valorDesconto!=null) {
					financeiroVO.setProperty("VLRDESC", valorDesconto);
					if(valorBoleto!=null) {
						financeiroVO.setProperty("VLRDESDOB", valorBoleto.subtract(valorDesconto));
					}
				}
				
			}
			
		}
		
	}

	
	// Metodos privados 
	
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
	
	private DynamicVO item(BigDecimal nunota) throws Exception {
		JapeWrapper DAO = JapeFactory.dao("ItemNota");
		DynamicVO VO = DAO.findOne("this.NUNOTA=? AND this.CODPROD=?",new Object[] { nunota, new BigDecimal(8) });
		return VO;
	}
	
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
	
	private Long dayToMiliseconds(int days){
	    Long result = Long.valueOf(days * 24 * 60 * 60 * 1000);
	    return result;
	}

	
	private Timestamp addDays(int days, Timestamp t1) throws Exception{
	    if(days < 0){
	        throw new Exception("Day in wrong format.");
	    }
	    Long miliseconds = dayToMiliseconds(days);
	    return new Timestamp(t1.getTime() + miliseconds);
	}
	
	private Timestamp getTimestamp(java.util.Date date)	{ 
		return date == null ? null : new java.sql.Timestamp(date.getTime()); 
	}

	
	private Timestamp getPrimeiroDiaDoMes(Date data) {
        Calendar c = Calendar.getInstance();
        c.setTime(data);
        c.set(Calendar.DAY_OF_MONTH, c.getActualMinimum(Calendar.DAY_OF_MONTH));
        Timestamp Result = getTimestamp(c.getTime());
		return  Result;
	}

	private Timestamp addMonths (int months,Timestamp data) {
		Calendar c = Calendar.getInstance();
		c.setTime(data);
		c.add(Calendar.MONTH, months);
        Timestamp Result = getTimestamp(c.getTime());
		return Result; 
	}
	
	private Timestamp getNewData(DynamicVO ContratoVO,Date dataVencimento) throws Exception {
		Timestamp retorno = (Timestamp) dataVencimento;
		Timestamp primeiroDiaMes= getPrimeiroDiaDoMes( dataVencimento );
		int dias = ContratoVO.asInt("DIAPAG");

		Timestamp newDataVencimento = addDays(dias-1,primeiroDiaMes);
		retorno = newDataVencimento;
		
		System.out.println(" --> DataReferencia:"+ newDataVencimento.toString());
		
		Date hoje = new Date();
		System.out.println(" --> Hoje:"+ hoje.toString());		
		
		if (newDataVencimento.compareTo(hoje)<1) {
			Timestamp newDt = addMonths(1,newDataVencimento); 
			retorno = newDt;
			System.out.println(" --> NovaData +1 mes:"+ newDt.toString());
		}

		return retorno;
		
	}
	
	/*	
	private Date getUltimoDiaDoMes(Date data) {
        Calendar c = Calendar.getInstance();
        c.setTime(data);
        c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date Result = c.getTime();
		return Result;
	}
*/

	
	private Boolean validaContrato (DynamicVO ContratoVO) {
		boolean ok = false;
		
		if (ContratoVO.getProperty("AD_DIAFIXO")==null){
			return false;
		}
		
		if (ContratoVO.asString("AD_DIAFIXO").equals("S")) {
			ok = true;
		}
   // 	System.out.println(" --> Contrato:"+ContratoVO.asString("NUMCONTRATO")+" -> Dia Fixo?:"+ContratoVO.asString("AD_DIAFIXO")+" ---> OK:");
		return ok;
		
	}

	private Boolean validaFinanceiro (DynamicVO financeiroVO) {
		boolean ok = false;
		
		if (financeiroVO.getProperty("NUMCONTRATO")==null) {
			return false;
		}
		
		if (!financeiroVO.asBigDecimal("NUMCONTRATO").equals(new java.math.BigDecimal(0))) {
			ok = true;
		}
		
		if (financeiroVO.asBigDecimal("CODTIPOPER").equals(new java.math.BigDecimal(1110)) && ok){
			ok = true;
			
		} else {
			ok = false;
		}
		
   // 	System.out.println(" --> Nufin:"+financeiroVO.asString("NUFIN")+" -> Contrato:"+financeiroVO.asString("NUMCONTRATO")+" TOP:"+financeiroVO.asString("CODTIPOPER")+" ---> OK:");
		return ok;
		
	}
	
	private void salvarException(String mensagem) {
		try {

			EntityFacade dwfFacade = EntityFacadeFactory.getDWFFacade();
			EntityVO NPVO = dwfFacade.getDefaultValueObjectInstance("AD_EXCEPTIONS");
			DynamicVO VO = (DynamicVO) NPVO;

			VO.setProperty("OBJETO", "eventoContratoDataFixa");
			VO.setProperty("PACOTE", "br.com.gc [Estácio]");
			VO.setProperty("DTEXCEPTION", TimeUtils.getNow());
			VO.setProperty("CODUSU", ((AuthenticationInfo) ServiceContext.getCurrent().getAutentication()).getUserID());
			VO.setProperty("ERRO", mensagem);

			dwfFacade.createEntity("AD_EXCEPTIONS", (EntityVO) VO);

		} catch (Exception e) {
			// aqui não tem jeito rs tem que mostrar no log
			System.out.println("## [btn_cadastrarLoja] ## - Nao foi possivel salvar a Exception! " + e.getMessage());
		}
	}
	

}



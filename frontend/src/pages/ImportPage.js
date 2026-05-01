import { renderSidebar } from '../components/Sidebar.js';
import { showToast } from '../components/Toast.js';
import { importApi } from '../api/importApi.js';

export function ImportPage() {
    return `
        ${renderSidebar('/import')}
        <main class="app-content">
            <div class="page-header" style="display: flex; justify-content: space-between; align-items: center;">
                <div>
                    <h1>Importação de Dados</h1>
                    <p>Faça upload de suas planilhas para alimentar o sistema</p>
                </div>
                <button class="btn btn-secondary" id="btn-download-template">
                    <i class="fas fa-file-download"></i> Baixar Template CSV
                </button>
            </div>

            <div class="card fade-in-up" style="max-width: 600px; margin: 0 auto; text-align: center; padding: var(--space-8);">
                <div id="drop-zone" style="
                    border: 2px dashed var(--glass-border);
                    border-radius: 12px;
                    padding: var(--space-12);
                    transition: all 0.3s ease;
                    cursor: pointer;
                    background: var(--bg-tertiary);
                ">
                    <i class="fas fa-cloud-upload-alt" style="font-size: 3rem; color: var(--accent-primary); margin-bottom: var(--space-4);"></i>
                    <h3>Arraste e solte sua planilha aqui</h3>
                    <p style="color: var(--text-secondary); margin-bottom: var(--space-4);">ou clique para selecionar o arquivo</p>
                    <p style="font-size: 0.8rem; color: var(--text-secondary);">Formatos suportados: .csv, .xlsx</p>
                    <input type="file" id="file-input" accept=".csv, .xlsx" style="display: none;" />
                </div>

                <div id="upload-status" style="display: none; margin-top: var(--space-6);">
                    <div class="loading-spinner" style="margin: 0 auto var(--space-3);"></div>
                    <p id="upload-status-text" style="color: var(--accent-secondary);">Processando arquivo...</p>
                </div>
            </div>
            
            <div class="card fade-in-up" style="max-width: 600px; margin: var(--space-6) auto; background: rgba(99, 102, 241, 0.05); border-color: rgba(99, 102, 241, 0.2);">
                <h4 style="color: var(--accent-primary); margin-bottom: var(--space-3);"><i class="fas fa-info-circle"></i> Como funciona a Auto-Detecção?</h4>
                <p style="font-size: 0.9rem; line-height: 1.5; color: var(--text-secondary);">
                    Nossa inteligência lê as colunas da sua planilha. Se você preencher a coluna <strong>"NomeProduto"</strong> e <strong>"PrecoVendaProduto"</strong>, o sistema automaticamente registrará esse novo produto no seu catálogo caso ele ainda não exista, vinculando-o à transação importada.
                </p>
            </div>
        </main>
    `;
}

export function initImportPage() {
    const dropZone = document.getElementById('drop-zone');
    const fileInput = document.getElementById('file-input');
    const btnDownload = document.getElementById('btn-download-template');
    
    if (btnDownload) {
        btnDownload.addEventListener('click', async () => {
            try {
                await importApi.downloadTemplate();
            } catch (error) {
                showToast(error.message, 'error');
            }
        });
    }

    if (dropZone && fileInput) {
        dropZone.addEventListener('click', () => fileInput.click());

        dropZone.addEventListener('dragover', (e) => {
            e.preventDefault();
            dropZone.style.borderColor = 'var(--accent-primary)';
            dropZone.style.background = 'rgba(99, 102, 241, 0.1)';
        });

        dropZone.addEventListener('dragleave', () => {
            dropZone.style.borderColor = 'var(--glass-border)';
            dropZone.style.background = 'var(--bg-tertiary)';
        });

        dropZone.addEventListener('drop', (e) => {
            e.preventDefault();
            dropZone.style.borderColor = 'var(--glass-border)';
            dropZone.style.background = 'var(--bg-tertiary)';
            
            if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
                handleFileUpload(e.dataTransfer.files[0]);
            }
        });

        fileInput.addEventListener('change', (e) => {
            if (e.target.files && e.target.files.length > 0) {
                handleFileUpload(e.target.files[0]);
            }
        });
    }
}

async function handleFileUpload(file) {
    const statusDiv = document.getElementById('upload-status');
    const statusText = document.getElementById('upload-status-text');
    const dropZone = document.getElementById('drop-zone');
    
    dropZone.style.display = 'none';
    statusDiv.style.display = 'block';
    statusText.textContent = `Enviando ${file.name}...`;

    try {
        const response = await importApi.upload(file);
        statusText.innerHTML = `<i class="fas fa-check-circle"></i> Sucesso! ${response.rowsDetected} linhas enviadas para processamento em segundo plano.`;
        showToast('Importação iniciada', 'success');
        
        setTimeout(() => {
            dropZone.style.display = 'block';
            statusDiv.style.display = 'none';
            document.getElementById('file-input').value = '';
        }, 3000);
        
    } catch (error) {
        statusDiv.style.display = 'none';
        dropZone.style.display = 'block';
        showToast(error.message, 'error');
    }
}

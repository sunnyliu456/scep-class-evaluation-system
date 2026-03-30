import axios from 'axios'

const http = axios.create({
  baseURL: '',
  timeout: 60000
})

export default http
